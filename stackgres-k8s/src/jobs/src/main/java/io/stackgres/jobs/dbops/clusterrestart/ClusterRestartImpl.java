/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.jobs.mutiny.UniUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRestartImpl implements ClusterRestart {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRestartImpl.class);

  private final PodRestart podRestart;

  private final ClusterSwitchoverHandler switchoverHandler;

  private final ClusterInstanceManager clusterInstanceManager;

  private final ClusterWatcher clusterWatcher;

  private final PostgresRestart postgresRestart;

  private final ExecutorService restartExecutor;

  @Inject
  public ClusterRestartImpl(PodRestart podRestart,
      ClusterSwitchoverHandler switchoverHandler,
      ClusterInstanceManager clusterInstanceManager,
      ClusterWatcher clusterWatcher,
      PostgresRestart postgresRestart) {
    this.podRestart = podRestart;
    this.switchoverHandler = switchoverHandler;
    this.clusterInstanceManager = clusterInstanceManager;
    this.clusterWatcher = clusterWatcher;
    this.postgresRestart = postgresRestart;
    this.restartExecutor = Executors.newSingleThreadExecutor(t -> new Thread(t, "RestartExecutor"));
  }

  @Override
  public Multi<RestartEvent> restartCluster(ClusterRestartState clusterRestartState) {
    return Multi.createFrom().emitter(em -> {
      try {
        var restartUni = restartCluster(clusterRestartState, em::emit)
            .runSubscriptionOn(restartExecutor);
        UniUtil.waitUniIndefinitely(restartUni);
        em.complete();
      } catch (Exception ex) {
        em.fail(ex);
      }
    });
  }

  private Uni<Object> restartCluster(ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    Pod primaryInstance = clusterRestartState.getPrimaryInstance();
    final String clusterName = clusterRestartState.getClusterName();
    final String namespace = clusterRestartState.getNamespace();
    final String primaryInstanceName = primaryInstance.getMetadata().getName();
    LOGGER.info("Checking if primary instance {} is available", primaryInstanceName);
    return clusterWatcher.getAvailablePrimary(clusterName, namespace)
        .chain(foundPrimaryInstanceName -> {
          if (foundPrimaryInstanceName
              .map(podName -> !primaryInstanceName.equals(podName))
              .orElse(false)) {
            String errorMessage = String.format("Primary instance %s changed from %s",
                foundPrimaryInstanceName.get(), primaryInstanceName);
            LOGGER.info(errorMessage);
            em.accept(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.PRIMARY_CHANGED)
                .build());
            return Uni.createFrom().failure(new RuntimeException(errorMessage));
          }
          if (foundPrimaryInstanceName.isEmpty()) {
            LOGGER.info("Primary instance not available");
            em.accept(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.PRIMARY_NOT_AVAILABLE)
                .build());
            return restartPodOfPrimaryInstance(clusterRestartState, em,
                primaryInstance, primaryInstanceName)
                .chain(() -> restartPodOfReplicas(clusterRestartState, em, primaryInstance));
          }
          LOGGER.info("Primary instance {} available", primaryInstanceName);
          em.accept(ImmutableRestartEvent.builder()
              .pod(primaryInstance)
              .eventType(RestartEventType.PRIMARY_AVAILABLE)
              .build());
          return restartPostgres(clusterRestartState, em, primaryInstance,
              primaryInstanceName, clusterName)
              .chain(() -> increaseClusterInstance(clusterRestartState, em, clusterName))
              .chain(() -> restartPodOfReplicas(clusterRestartState, em, primaryInstance))
              .chain(() -> performSwitchover(
                  clusterRestartState, em, primaryInstance, clusterName))
              .chain(() -> restartPodOfPrimaryInstance(clusterRestartState, em,
                  primaryInstance, primaryInstanceName))
              .chain(() -> decreaseClusterInstance(clusterRestartState, em, clusterName));
        });
  }

  private Uni<?> restartPostgres(ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em, Pod primaryInstance,
      final String primaryInstanceName, final String clusterName) {
    if (clusterRestartState.getRestartedInstances().isEmpty()
        && clusterRestartState.hasToBeRestarted(primaryInstance)) {
      return Uni.createFrom().voidItem()
          .invoke(() -> {
            LOGGER.info("Restarting postgres of primary node {} of cluster {}",
                primaryInstance.getMetadata().getName(),
                clusterName);
            em.accept(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.RESTARTING_POSTGRES)
                .build());
          })
          .chain((a) -> postgresRestart.restartPostgres(primaryInstanceName,
              clusterName,
              clusterRestartState.getNamespace()))
          .onItemOrFailure()
          .invoke((restarted, failure) -> checkPostgresRestart(em, primaryInstance,
              primaryInstanceName, failure))
          .onFailure()
          .transform(failure -> new FailedRestartPostgresException(
              format("Restart of instance %s failed", primaryInstanceName),
                  failure))
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().voidItem();
  }

  private void checkPostgresRestart(
      Consumer<RestartEvent> em,
      Pod primaryInstance, String primaryInstanceName,
      Throwable failure) {
    if (failure == null) {
      LOGGER.info("Restart of instance {} completed", primaryInstanceName);
      em.accept(ImmutableRestartEvent.builder()
          .pod(primaryInstance)
          .eventType(RestartEventType.POSTGRES_RESTARTED)
          .build());
    } else {
      em.accept(ImmutableRestartEvent.builder()
          .pod(primaryInstance)
          .eventType(RestartEventType.POSTGRES_RESTART_FAILED)
          .build());
    }
  }

  private Uni<?> increaseClusterInstance(ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em, final String clusterName) {
    if (isReducedImpact(clusterRestartState)
        && hasInstancesNotBeenIncreased(clusterRestartState)) {
      return Uni.createFrom().voidItem()
          .onItem()
          .invoke(() -> {
            LOGGER.info("Increasing instances of cluster {}", clusterName);
            em.accept(ImmutableRestartEvent.builder()
                .eventType(RestartEventType.INCREASING_INSTANCES)
                .build());
          })
          .chain(() -> clusterInstanceManager.increaseClusterInstances(
              clusterName,
              clusterRestartState.getNamespace()))
          .onItem().invoke((createdPod) -> {
            LOGGER.info("Instances of cluster {} increased", clusterName);
            em.accept(ImmutableRestartEvent.builder()
                .pod(createdPod)
                .eventType(RestartEventType.INSTANCES_INCREASED)
                .build());
          })
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().voidItem();
  }

  private Uni<?> restartPodOfReplicas(ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em, Pod primaryInstance) {
    List<Pod> replicas = clusterRestartState.getInitialInstances().stream()
        .filter(pod -> !primaryInstance.equals(pod))
        .filter(clusterRestartState::hasToBeRestarted)
        .collect(Collectors.toUnmodifiableList());

    var restartReplicas = Uni.createFrom().nullItem();
    for (Pod replica : replicas) {
      restartReplicas = restartReplicas
          .onItem()
          .invoke(() -> logPodRestartReason(replica, clusterRestartState))
          .invoke(() -> {
            LOGGER.info("Restarting replica pod {}", replica.getMetadata().getName());
            em.accept(ImmutableRestartEvent.builder()
                .pod(replica)
                .eventType(RestartEventType.RESTARTING_POD)
                .build());
          })
          .chain(() -> podRestart.restartPod(clusterRestartState.getClusterName(), replica))
          .onItem()
          .invoke(() -> {
            LOGGER.info("Pod {} restarted", replica.getMetadata().getName());
            em.accept(ImmutableRestartEvent.builder()
                .pod(replica)
                .eventType(RestartEventType.POD_RESTARTED)
                .build());
          })
          .onFailure()
          .retry()
          .indefinitely()
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return restartReplicas;
  }

  private Uni<?> performSwitchover(ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em, Pod primaryInstance,
      final String clusterName) {
    if (!clusterRestartState.isSwitchoverFinalized()
        && clusterRestartState.hasToBeRestarted(primaryInstance)) {
      return Uni.createFrom().nullItem()
          .onItem()
          .invoke(() -> em.accept(ImmutableRestartEvent.builder()
              .pod(primaryInstance)
              .eventType(RestartEventType.SWITCHOVER_INITIATED)
              .build()))
          .onItem()
          .invoke(() -> LOGGER.info("Performing Switchover over cluster {}", clusterName))
          .chain(() -> switchoverHandler.performSwitchover(
              clusterRestartState.getPrimaryInstance().getMetadata().getName(),
              clusterName, clusterRestartState.getNamespace()))
          .onItem()
          .invoke(() -> em.accept(ImmutableRestartEvent.builder()
              .pod(primaryInstance)
              .eventType(RestartEventType.SWITCHOVER_FINALIZED)
              .build()))
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().nullItem();
  }

  private Uni<?> restartPodOfPrimaryInstance(ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em, Pod primaryInstance,
      final String primaryInstanceName) {
    if (clusterRestartState.hasToBeRestarted(primaryInstance)) {
      return Uni.createFrom().voidItem()
          .onItem()
          .invoke(() -> logPodRestartReason(primaryInstance, clusterRestartState))
          .invoke(() -> {
            LOGGER.info("Restarting primary pod {}", primaryInstanceName);
            em.accept(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.RESTARTING_POD)
                .build());
          })
          .chain(() -> podRestart.restartPod(
              clusterRestartState.getClusterName(), primaryInstance))
          .onItem()
          .invoke(() -> {
            LOGGER.info("Pod {} restarted", primaryInstanceName);
            em.accept(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.POD_RESTARTED)
                .build());
          })
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().voidItem();
  }

  private Uni<?> decreaseClusterInstance(ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em, final String clusterName) {
    if (isReducedImpact(clusterRestartState)
        && hasInstancesNotBeenDecreased(clusterRestartState)) {
      return Uni.createFrom().voidItem()
          .onItem()
          .invoke(() -> {
            LOGGER.info("Decreasing instances of cluster {}", clusterName);
            em.accept(ImmutableRestartEvent.builder()
                .eventType(RestartEventType.DECREASING_INSTANCES)
                .build());
          })
          .chain(() -> clusterInstanceManager.decreaseClusterInstances(clusterName,
              clusterRestartState.getNamespace()))
          .onItem()
          .invoke(() -> {
            LOGGER.info("Instances of cluster {} decreased", clusterName);
            em.accept(ImmutableRestartEvent.builder()
                .eventType(RestartEventType.INSTANCES_DECREASED)
                .build());
          });
    }
    return Uni.createFrom().voidItem();
  }

  private Uni<StackGresCluster> waitForClusterToBeHealthy(ClusterRestartState clusterRestartState) {
    String clusterName = clusterRestartState.getClusterName();
    return Uni.createFrom().voidItem()
        .onItem().invoke(() -> LOGGER.info("Waiting for cluster {} to be healthy", clusterName))
        .chain(() -> clusterWatcher.waitUntilIsReady(
            clusterName, clusterRestartState.getNamespace()))
        .onItem().invoke(() -> LOGGER.info("Cluster {} healthy", clusterName));
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
    return clusterRestartState.getRestartMethod().equals(DbOpsMethodType.REDUCED_IMPACT);
  }

  private RestartReasons getRestartReasons(ClusterRestartState clusterRestartState, Pod pod) {
    return clusterRestartState.getPodRestartReasonsMap().get(pod);
  }
}
