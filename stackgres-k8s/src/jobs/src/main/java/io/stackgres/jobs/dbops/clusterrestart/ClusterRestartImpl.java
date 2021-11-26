/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRestartImpl implements ClusterRestart {

  public static final String REDUCED_IMPACT_METHOD = "ReducedImpact";
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRestartImpl.class);
  private final PodRestart podRestart;

  private final ClusterSwitchoverHandler switchoverHandler;

  private final ClusterInstanceManager clusterInstanceManager;

  private final Watcher<StackGresCluster> clusterWatcher;

  private final PostgresRestart postgresRestart;

  @Inject
  public ClusterRestartImpl(PodRestart podRestart,
      ClusterSwitchoverHandler switchoverHandler,
      ClusterInstanceManager clusterInstanceManager,
      Watcher<StackGresCluster> clusterWatcher,
      PostgresRestart postgresRestart) {
    this.podRestart = podRestart;
    this.switchoverHandler = switchoverHandler;
    this.clusterInstanceManager = clusterInstanceManager;
    this.clusterWatcher = clusterWatcher;
    this.postgresRestart = postgresRestart;
  }

  @Override
  public Multi<RestartEvent> restartCluster(ClusterRestartState clusterRestartState) {
    return Multi.createFrom().emitter(em -> {

      Pod primaryInstance = clusterRestartState.getPrimaryInstance();
      final String primaryInstanceName = primaryInstance.getMetadata().getName();
      final String clusterName = clusterRestartState.getClusterName();

      Uni<?> restartChain = startRestartChain();

      restartChain = restartPostgres(clusterRestartState, em, restartChain, primaryInstance,
          primaryInstanceName, clusterName);

      restartChain = increaseClusterInstance(clusterRestartState, em, restartChain, clusterName);

      restartChain = restartPodOfReplicas(clusterRestartState, em, restartChain, primaryInstance);

      restartChain =
          performSwitchover(clusterRestartState, em, restartChain, primaryInstance, clusterName);

      restartChain = restartPodOfPrimaryInstance(clusterRestartState, em, restartChain,
          primaryInstance, primaryInstanceName);

      restartChain = decreaseClusterInstance(clusterRestartState, em, restartChain, clusterName);

      restartChain.subscribe().with((x) -> em.complete());
    });
  }

  private Uni<?> decreaseClusterInstance(ClusterRestartState clusterRestartState,
      MultiEmitter<? super RestartEvent> em, Uni<?> restartChain, final String clusterName) {
    if (isReducedImpact(clusterRestartState)
        && hasInstancesNotBeenDecreased(clusterRestartState)) {
      restartChain = restartChain.onItem()
          .invoke(() -> {
            LOGGER.info("Decreasing instances of cluster {}", clusterName);
            em.emit(ImmutableRestartEvent.builder()
                .eventType(RestartEventType.DECREASING_INSTANCES)
                .build());
          })
          .chain(() -> clusterInstanceManager.decreaseClusterInstances(clusterName,
              clusterRestartState.getNamespace()))
          .onItem()
          .invoke(() -> {
            LOGGER.info("Instances of cluster {} decreased", clusterName);
            em.emit(ImmutableRestartEvent.builder()
                .eventType(RestartEventType.INSTANCES_DECREASED)
                .build());
          });
    }
    return restartChain;
  }

  private Uni<?> restartPodOfPrimaryInstance(ClusterRestartState clusterRestartState,
      MultiEmitter<? super RestartEvent> em, Uni<?> restartChain, Pod primaryInstance,
      final String primaryInstanceName) {
    if (clusterRestartState.hasToBeRestarted(primaryInstance)) {
      restartChain = restartChain.onItem()
          .invoke(() -> logPodRestartReason(primaryInstance, clusterRestartState))
          .invoke(() -> {
            LOGGER.info("Restarting pod {}", primaryInstanceName);
            em.emit(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.RESTARTING_POD)
                .build());
          })
          .chain(() -> podRestart.restartPod(primaryInstance))
          .onItem()
          .invoke(() -> {
            LOGGER.info("Pod {} restarted", primaryInstanceName);
            em.emit(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.POD_RESTARTED)
                .build());
          });

      restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
    }
    return restartChain;
  }

  private Uni<?> performSwitchover(ClusterRestartState clusterRestartState,
      MultiEmitter<? super RestartEvent> em, Uni<?> restartChain, Pod primaryInstance,
      final String clusterName) {
    if (!clusterRestartState.isSwitchoverFinalized()
        && clusterRestartState.hasToBeRestarted(primaryInstance)) {
      restartChain = restartChain
          .onItem()
          .invoke(() -> em.emit(ImmutableRestartEvent.builder()
              .pod(primaryInstance)
              .eventType(RestartEventType.SWITCHOVER_INITIATED)
              .build()))
          .onItem()
          .invoke(() -> LOGGER.info("Performing Switchover over cluster {}", clusterName))
          .chain(() -> switchoverHandler.performSwitchover(
              clusterRestartState.getPrimaryInstance().getMetadata().getName(),
              clusterName, clusterRestartState.getNamespace()))
          .onItem()
          .invoke(() -> em.emit(ImmutableRestartEvent.builder()
              .pod(primaryInstance)
              .eventType(RestartEventType.SWITCHOVER_FINALIZED)
              .build()));

      restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
    }
    return restartChain;
  }

  private Uni<?> restartPodOfReplicas(ClusterRestartState clusterRestartState,
      MultiEmitter<? super RestartEvent> em, Uni<?> restartChain, Pod primaryInstance) {
    List<Pod> replicas = clusterRestartState.getInitialInstances().stream()
        .filter(pod -> !primaryInstance.equals(pod))
        .filter(clusterRestartState::hasToBeRestarted)
        .collect(Collectors.toUnmodifiableList());

    for (Pod replica : replicas) {
      restartChain = restartChain
          .onItem()
          .invoke(() -> logPodRestartReason(replica, clusterRestartState))
          .invoke(() -> {
            LOGGER.info("Restarting pod {}", replica.getMetadata().getName());
            em.emit(ImmutableRestartEvent.builder()
                .pod(replica)
                .eventType(RestartEventType.RESTARTING_POD)
                .build());
          })
          .chain(() -> podRestart.restartPod(replica))
          .onItem()
          .invoke(() -> {
            LOGGER.info("Pod {} restarted", replica.getMetadata().getName());
            em.emit(ImmutableRestartEvent.builder()
                .pod(replica)
                .eventType(RestartEventType.POD_RESTARTED)
                .build());
          }).onFailure().retry().indefinitely();

      restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
    }
    return restartChain;
  }

  private Uni<?> increaseClusterInstance(ClusterRestartState clusterRestartState,
      MultiEmitter<? super RestartEvent> em, Uni<?> restartChain, final String clusterName) {
    if (isReducedImpact(clusterRestartState)
        && hasInstancesNotBeenIncreased(clusterRestartState)) {

      restartChain = restartChain.onItem()
          .invoke(() -> {
            LOGGER.info("Increasing instances of cluster {}", clusterName);
            em.emit(ImmutableRestartEvent.builder()
                .eventType(RestartEventType.INCREASING_INSTANCES)
                .build());
          })
          .chain(() -> clusterInstanceManager.increaseClusterInstances(
              clusterName,
              clusterRestartState.getNamespace()))
          .onItem().invoke((createdPod) -> {
            LOGGER.info("Instances of cluster {} increased", clusterName);
            em.emit(ImmutableRestartEvent.builder()
                .pod(createdPod)
                .eventType(RestartEventType.INSTANCES_INCREASED)
                .build());
          });

      restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
    }
    return restartChain;
  }

  private Uni<?> restartPostgres(ClusterRestartState clusterRestartState,
      MultiEmitter<? super RestartEvent> em, Uni<?> restartChain, Pod primaryInstance,
      final String primaryInstanceName, final String clusterName) {

    if (clusterRestartState.getRestartedInstances().isEmpty()
        && clusterRestartState.hasToBeRestarted(primaryInstance)) {
      restartChain = restartChain
          .invoke(() -> {
            LOGGER.info("Restarting primary node postgres of cluster {}",
                clusterName);
            em.emit(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.RESTARTING_POSTGRES)
                .build());
          })
          .chain((a) -> postgresRestart.restartPostgres(primaryInstanceName,
              clusterName,
              clusterRestartState.getNamespace()))
          .onItemOrFailure()
          .invoke((restarted, failure) -> checkPostgresRestart(restarted, em, primaryInstance,
              primaryInstanceName));
      restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
    }
    return restartChain;
  }

  private Object checkPostgresRestart(Boolean isPostgresRestarted,
      MultiEmitter<? super RestartEvent> em,
      Pod primaryInstance, String primaryInstanceName) {
    {
      if (isPostgresRestarted) {
        LOGGER.info(format("Postgres of instance {} restarted", primaryInstanceName));
        em.emit(ImmutableRestartEvent.builder()
            .pod(primaryInstance)
            .eventType(RestartEventType.POSTGRES_RESTARTED)
            .build());
      } else {
        LOGGER.info("Postgres of instance {} failed", primaryInstanceName);
        em.emit(ImmutableRestartEvent.builder()
            .pod(primaryInstance)
            .eventType(RestartEventType.POSTGRES_RESTART_FAILED)
            .build());
        em.fail(new FailedRestartPostgresException(
            format("Postgres of instance %s failed", primaryInstanceName)));
      }
    }
    return null;
  }

  private void logPodRestartReason(Pod pod, ClusterRestartState clusterRestartState) {
    if (LOGGER.isInfoEnabled()) {
      RestartReasons reasons = getRestartReasons(clusterRestartState, pod);
      for (RestartReason reason : reasons.getReasons()) {
        switch (reason) {
          case OPERATOR_VERSION:
            LOGGER.info("Pod {} requires restart due to operator version change",
                pod.getMetadata().getName());
            break;
          case PATRONI:
            LOGGER.info("Pod {} requires restart due to patroni's indication",
                pod.getMetadata().getName());
            break;
          case POD_STATUS:
            LOGGER.info("Pod {} requires restart due to pod status indication",
                pod.getMetadata().getName());
            break;
          case STATEFULSET:
            LOGGER.info("Pod {} requires restart due to pod template changes",
                pod.getMetadata().getName());
            break;
          default:
            break;
        }
      }
    }
  }

  private Uni<Void> startRestartChain() {
    return Uni.createFrom().voidItem();
  }

  private Uni<StackGresCluster> waitForClusterToBeHealthy(ClusterRestartState clusterRestartState,
      Uni<?> chain) {
    String clusterName = clusterRestartState.getClusterName();
    return chain
        .onItem().invoke(() -> LOGGER.info("Waiting for cluster {} to be healthy", clusterName))
        .chain(() -> clusterWatcher.waitUntilIsReady(
            clusterName, clusterRestartState.getNamespace()))
        .onItem().invoke(() -> LOGGER.info("Cluster {} healthy", clusterName));
  }

  private boolean hasInstancesNotBeenIncreased(ClusterRestartState clusterRestartState) {
    final int totalInstances = clusterRestartState.getTotalInstances().size();
    final int initialInstances = clusterRestartState.getInitialInstances().size();
    return totalInstances == initialInstances
        && clusterRestartState.getRestartedInstances().isEmpty();
  }

  private boolean hasInstancesNotBeenDecreased(ClusterRestartState clusterRestartState) {
    final int totalInstances = clusterRestartState.getTotalInstances().size();
    final int initialInstances = clusterRestartState.getInitialInstances().size();
    return totalInstances > initialInstances
        || (totalInstances == initialInstances
            && clusterRestartState.getRestartedInstances().isEmpty());
  }

  private boolean isReducedImpact(ClusterRestartState clusterRestartState) {
    return clusterRestartState.getRestartMethod().equals(REDUCED_IMPACT_METHOD);
  }

  private RestartReasons getRestartReasons(ClusterRestartState clusterRestartState, Pod pod) {
    return clusterRestartState.getPodRestartReasonsMap().get(pod);
  }
}
