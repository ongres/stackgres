/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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
      Uni<?> restartChain = waitForClusterToBeHealthy(clusterRestartState);

      Pod primaryInstance = clusterRestartState.getPrimaryInstance();

      final String primaryInstanceName = primaryInstance.getMetadata().getName();

      final String clusterName = clusterRestartState.getClusterName();

      if (clusterRestartState.getRestartedInstances().isEmpty()
          && clusterRestartState.hasToBeRestarted(primaryInstance)) {
        restartChain = restartChain
            .invoke(() -> LOGGER.info("Restarting primary node postgres of cluster {}",
                clusterName))
            .chain(() -> postgresRestart.restartPostgres(primaryInstanceName,
                clusterName,
                clusterRestartState.getNamespace()))
            .onItem().invoke(() -> {
              LOGGER.info("Postgres of instance {} restarted", primaryInstanceName);
              em.emit(ImmutableRestartEvent.builder()
                  .pod(primaryInstance)
                  .eventType(RestartEventType.POSTGRES_RESTART)
                  .build());
            });
        restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
      }

      if (isReducedImpact(clusterRestartState)
          && hasInstancesNotBeenIncreased(clusterRestartState)) {

        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Increasing instances of cluster {}", clusterName))
            .chain(() ->
                clusterInstanceManager.increaseClusterInstances(
                    clusterName,
                    clusterRestartState.getNamespace())

            ).onItem().invoke((createdPod) -> {
              LOGGER.info("Instances of cluster {} increased", clusterName);
              em.emit(ImmutableRestartEvent.builder()
                  .pod(createdPod)
                  .eventType(RestartEventType.POD_CREATED)
                  .build());
            });

        restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
      }

      List<Pod> replicas = clusterRestartState.getInitialInstances().stream()
          .filter(pod -> !primaryInstance.equals(pod))
          .filter(pod -> clusterRestartState.hasToBeRestarted(pod))
          .collect(Collectors.toUnmodifiableList());

      for (Pod replica : replicas) {
        restartChain = restartChain
            .onItem()
            .invoke(() -> logPodRestartReason(replica, clusterRestartState))
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

        restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
      }

      if (!clusterRestartState.isSwitchoverInitiated()
          && clusterRestartState.hasToBeRestarted(primaryInstance)) {
        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Performing Switchover over cluster {}", clusterName))
            .chain(() ->
                switchoverHandler.performSwitchover(
                    clusterRestartState.getPrimaryInstance().getMetadata().getName(),
                    clusterName, clusterRestartState.getNamespace())
            ).onItem()
            .invoke(() -> em.emit(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.SWITCHOVER)
                .build()));

        restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
      }

      if (clusterRestartState.hasToBeRestarted(primaryInstance)) {
        restartChain = restartChain.onItem()
            .invoke(() -> logPodRestartReason(primaryInstance, clusterRestartState))
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

        restartChain = waitForClusterToBeHealthy(clusterRestartState, restartChain);
      }

      if (isReducedImpact(clusterRestartState)
          && hasInstancesNotBeenDecreased(clusterRestartState)) {
        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Decreasing instances of cluster {}", clusterName))
            .chain(() -> clusterInstanceManager.decreaseClusterInstances(clusterName,
                clusterRestartState.getNamespace()))
            .onItem()
            .invoke(() -> LOGGER.info("Instances of cluster {} decreased", clusterName));
      }

      restartChain.subscribe().with((x) -> em.complete());
    });
  }

  private void logPodRestartReason(Pod pod, ClusterRestartState clusterRestartState) {
    RestartReasons reasons = clusterRestartState.getRestartReasons(pod);
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

  private Uni<StackGresCluster> waitForClusterToBeHealthy(ClusterRestartState clusterRestartState) {
    String clusterName = clusterRestartState.getClusterName();
    LOGGER.info("Waiting for cluster {} to be healthy", clusterName);
    return clusterWatcher.waitUntilIsReady(clusterName, clusterRestartState.getNamespace())
        .onItem().invoke(() -> LOGGER.info("Cluster {} healthy", clusterName));
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
}
