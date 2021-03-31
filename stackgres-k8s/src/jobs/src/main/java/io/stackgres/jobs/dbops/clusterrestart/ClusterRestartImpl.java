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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRestartImpl implements ClusterRestart {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRestartImpl.class);

  private static final String REDUCED_IMPACT_METHOD = "ReducedImpact";
  private final PodRestart podRestart;

  private final ClusterSwitchoverHandler switchoverHandler;

  private final ClusterInstanceManager clusterInstanceManager;

  private final Watcher<StackGresCluster> clusterWatcher;

  @Inject
  public ClusterRestartImpl(PodRestart podRestart,
                            ClusterSwitchoverHandler switchoverHandler,
                            ClusterInstanceManager clusterInstanceManager,
                            Watcher<StackGresCluster> clusterWatcher) {
    this.podRestart = podRestart;
    this.switchoverHandler = switchoverHandler;
    this.clusterInstanceManager = clusterInstanceManager;
    this.clusterWatcher = clusterWatcher;
  }

  @Override
  public Multi<RestartEvent> restartCluster(ClusterRestartState clusterState) {

    return Multi.createFrom().emitter(em -> {

      Uni<?> restartChain = waitForClusterToBeHealthy(clusterState);

      Pod primaryInstance = clusterState.getPrimaryInstance();

      final String clusterName = clusterState.getClusterName();
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

      List<Pod> replicas = clusterState.getTotalInstances().stream()
          .filter(pod -> !primaryInstance.equals(pod))
          .filter(pod -> !clusterState.getRestartedInstances().contains(pod))
          .collect(Collectors.toUnmodifiableList());

      for (Pod replica : replicas) {
        restartChain = restartChain
            .onItem()
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

      if (!clusterState.isSwitchoverInitiated()) {

        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Performing Switchover over cluster {}", clusterName))
            .chain(() ->
                switchoverHandler.performSwitchover(clusterName, clusterState.getNamespace())
            ).onItem()
            .invoke(() -> em.emit(ImmutableRestartEvent.builder()
                .pod(primaryInstance)
                .eventType(RestartEventType.SWITCHOVER)
                .build()));

        restartChain = waitForClusterToBeHealthy(clusterState, restartChain);

      }

      if (isPrimaryInstanceRestarted(clusterState, primaryInstance)) {

        restartChain = restartChain.onItem()
            .invoke(() -> LOGGER.info("Restarting pod {}", primaryInstance.getMetadata().getName()))
            .chain(() -> podRestart.restartPod(primaryInstance))
            .onItem()
            .invoke(() -> {
              LOGGER.info("Pod {} restarted", primaryInstance.getMetadata().getName());
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

  private boolean isPrimaryInstanceRestarted(ClusterRestartState clusterState, Pod primaryInstance) {
    return !clusterState.getRestartedInstances().contains(primaryInstance);
  }

  private boolean hasInstancesNotBeenIncreased(ClusterRestartState clusterState) {
    return clusterState.getTotalInstances().size() == clusterState.getInitialInstances().size()
        && clusterState.getRestartedInstances().isEmpty();
  }

  private boolean hasInstancesNotBeenDecreased(ClusterRestartState clusterState) {
    return clusterState.getTotalInstances().size() > clusterState.getInitialInstances().size()
        || (
        clusterState.getTotalInstances().size() == clusterState.getInitialInstances().size()
            && clusterState.getRestartedInstances().isEmpty());
  }

  private boolean isReducedImpact(ClusterRestartState clusterState) {
    return clusterState.getRestartMethod().equals(REDUCED_IMPACT_METHOD);
  }
}
