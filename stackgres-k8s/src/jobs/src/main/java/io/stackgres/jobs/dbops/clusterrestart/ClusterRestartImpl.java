/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterPendingRestartUtil;
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
  public Multi<RestartEvent> restartCluster(ClusterRestartState clusterState) {
    return Multi.createFrom().emitter(em -> {
      Uni<?> restartChain = waitForClusterToBeHealthy(clusterState);

      Pod primaryInstance = clusterState.getPrimaryInstance();

      final String primaryInstanceName = primaryInstance.getMetadata().getName();

      final String clusterName = clusterState.getClusterName();

      if (clusterState.getRestartedInstances().isEmpty()
          && hasPodToBeRestarted(primaryInstance, clusterState)) {
        restartChain = restartChain
            .invoke(() -> LOGGER.info("Restarting primary node postgres of cluster {}",
                clusterName))
            .chain(() -> postgresRestart.restartPostgres(primaryInstanceName,
                clusterName,
                clusterState.getNamespace()))
            .onItem().invoke(() -> {
              LOGGER.info("Postgres of instance {} restarted", primaryInstanceName);
              em.emit(ImmutableRestartEvent.builder()
                  .pod(primaryInstance)
                  .eventType(RestartEventType.POSTGRES_RESTART)
                  .build());
            });
        restartChain = waitForClusterToBeHealthy(clusterState, restartChain);
      }

      if (isReducedImpact(clusterState)
          && hasInstancesNotBeenIncreased(clusterState)) {

        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Increasing instances of cluster {}", clusterName))
            .chain(() ->
                clusterInstanceManager.increaseClusterInstances(
                    clusterName,
                    clusterState.getNamespace())

            ).onItem().invoke((createdPod) -> {
              LOGGER.info("Instances of cluster {} increased", clusterName);
              em.emit(ImmutableRestartEvent.builder()
                  .pod(createdPod)
                  .eventType(RestartEventType.POD_CREATED)
                  .build());
            });

        restartChain = waitForClusterToBeHealthy(clusterState, restartChain);
      }

      List<Pod> replicas = clusterState.getInitialInstances().stream()
          .filter(pod -> !primaryInstance.equals(pod))
          .filter(pod -> hasPodToBeRestarted(pod, clusterState))
          .collect(Collectors.toUnmodifiableList());

      for (Pod replica : replicas) {
        restartChain = restartChain
            .onItem()
            .invoke(() -> logPodRestartReason(replica, clusterState))
            .invoke(() -> LOGGER.info("Restarting pod {}", replica.getMetadata().getName()))
            .chain(() -> podRestart.restartPod(replica))
            .onItem()
            .invoke(() -> {
              LOGGER.info("Pod {} restarted", replica.getMetadata().getName());
              em.emit(ImmutableRestartEvent.builder()
                  .pod(replica)
                  .eventType(RestartEventType.POD_RESTART)
                  .build());
            });

        restartChain = waitForClusterToBeHealthy(clusterState, restartChain);
      }

      if (!clusterState.isSwitchoverInitiated()
          && hasPrimaryInstanceToBeRestarted(clusterState, primaryInstance)) {
        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Performing Switchover over cluster {}", clusterName))
            .chain(() ->
                switchoverHandler.performSwitchover(
                    clusterState.getPrimaryInstance().getMetadata().getName(),
                    clusterName, clusterState.getNamespace())
            ).onItem()
            .invoke(() -> em.emit(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.SWITCHOVER)
                .build()));

        restartChain = waitForClusterToBeHealthy(clusterState, restartChain);
      }

      if (hasPrimaryInstanceToBeRestarted(clusterState, primaryInstance)) {
        restartChain = restartChain.onItem()
            .invoke(() -> logPodRestartReason(primaryInstance, clusterState))
            .invoke(() -> LOGGER.info("Restarting pod {}", primaryInstanceName))
            .chain(() -> podRestart.restartPod(primaryInstance))
            .onItem()
            .invoke(() -> {
              LOGGER.info("Pod {} restarted", primaryInstanceName);
              em.emit(ImmutableRestartEvent.builder()
                  .pod(primaryInstance)
                  .eventType(RestartEventType.POD_RESTART)
                  .build());
            });

        restartChain = waitForClusterToBeHealthy(clusterState, restartChain);
      }

      if (isReducedImpact(clusterState) && hasInstancesNotBeenDecreased(clusterState)) {
        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Decreasing instances of cluster {}", clusterName))
            .chain(() -> clusterInstanceManager.decreaseClusterInstances(clusterName,
                clusterState.getNamespace()))
            .onItem()
            .invoke(() -> LOGGER.info("Instances of cluster {} decreased", clusterName));
      }

      restartChain.subscribe().with((x) -> em.complete());
    });
  }

  private boolean hasPodToBeRestarted(Pod pod, ClusterRestartState clusterState) {
    return !clusterState.getRestartedInstances().contains(pod)
        && (!clusterState.isOnlyPendingRrestart()
            || isPendingRestart(pod, clusterState));
  }

  private boolean isPendingRestart(Pod pod, ClusterRestartState clusterState) {
    return ClusterPendingRestartUtil.getRestartReasons(
        clusterState.getPodStatuses(),
        clusterState.getStatefulSet(),
        ImmutableList.of(pod)).requiresRestart();
  }

  private void logPodRestartReason(Pod pod, ClusterRestartState clusterState) {
    RestartReasons reasons = ClusterPendingRestartUtil.getRestartReasons(
        clusterState.getPodStatuses(),
        clusterState.getStatefulSet(),
        ImmutableList.of(pod));
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

  private Uni<StackGresCluster> waitForClusterToBeHealthy(ClusterRestartState clusterState) {
    String clusterName = clusterState.getClusterName();
    LOGGER.info("Waiting for cluster {} to be healthy", clusterName);
    return clusterWatcher.waitUntilIsReady(clusterName, clusterState.getNamespace())
        .onItem().invoke(() -> LOGGER.info("Cluster {} healthy", clusterName));
  }

  private Uni<StackGresCluster> waitForClusterToBeHealthy(ClusterRestartState clusterState,
                                                          Uni<?> chain) {
    String clusterName = clusterState.getClusterName();
    return chain
        .onItem().invoke(() -> LOGGER.info("Waiting for cluster {} to be healthy", clusterName))
        .chain(() -> clusterWatcher.waitUntilIsReady(clusterName, clusterState.getNamespace()))
        .onItem().invoke(() -> LOGGER.info("Cluster {} healthy", clusterName));
  }

  private boolean hasPrimaryInstanceToBeRestarted(ClusterRestartState clusterState,
                                             Pod primaryInstance) {
    return hasPodToBeRestarted(primaryInstance, clusterState);
  }

  private boolean hasInstancesNotBeenIncreased(ClusterRestartState clusterState) {
    return clusterState.getTotalInstances().size() == clusterState.getInitialInstances().size()
        && clusterState.getRestartedInstances().isEmpty();
  }

  private boolean hasInstancesNotBeenDecreased(ClusterRestartState clusterState) {
    return clusterState.getTotalInstances().size() > clusterState.getInitialInstances().size()
        ||
        clusterState.getTotalInstances().size() == clusterState.getInitialInstances().size()
            && clusterState.getRestartedInstances().isEmpty();
  }

  private boolean isReducedImpact(ClusterRestartState clusterState) {
    return clusterState.getRestartMethod().equals(REDUCED_IMPACT_METHOD);
  }
}
