/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterRolloutUtil.RestartReason;
import io.stackgres.common.ClusterRolloutUtil.RestartReasons;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRestart {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRestart.class);

  @Inject
  PodRestart podRestart;

  @Inject
  ClusterSwitchoverHandler switchoverHandler;

  @Inject
  ClusterInstanceManager clusterInstanceManager;

  @Inject
  ClusterWatcher clusterWatcher;

  @Inject
  PostgresRestart postgresRestart;

  @Inject
  DbOpsExecutorService executorService;

  public Multi<RestartEvent> restartCluster(ClusterRestartState clusterRestartState) {
    return Multi.createFrom()
        .<RestartEvent>emitter(em -> Uni.createFrom().voidItem()
            .emitOn(executorService.getExecutorService())
            .chain(() -> restartCluster(clusterRestartState, em::emit)
                .onItem()
                .invoke(em::complete)
                .onFailure()
                .invoke(em::fail))
            .await()
            .indefinitely());
  }

  private Uni<Object> restartCluster(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    em.accept(ImmutableRestartEventInstance.builder()
        .message(String.format("Checking if primary instance %s is available",
            clusterRestartState.getPrimaryInstance()))
        .eventType(RestartEventType.CHECK_PRIMARY_AVAILABLE)
        .build());
    return clusterWatcher.getAvailablePrimary(
            clusterRestartState.getClusterName(),
            clusterRestartState.getNamespace())
        .chain(foundPrimaryInstanceName -> {
          if (foundPrimaryInstanceName.isPresent()
              && !foundPrimaryInstanceName.equals(clusterRestartState.getPrimaryInstance())) {
            String message = String.format("Primary instance %s changed from %s",
                foundPrimaryInstanceName.orElse("<not found>"),
                clusterRestartState.getPrimaryInstance().orElse("<not found>"));
            LOGGER.info(message);
            em.accept(ImmutableRestartEventInstance.builder()
                .message(message)
                .eventType(RestartEventType.PRIMARY_CHANGED)
                .build());
            return Uni.createFrom().failure(new RuntimeException(message));
          }
          if (foundPrimaryInstanceName.isEmpty()) {
            em.accept(ImmutableRestartEventInstance.builder()
                .message("Primary instance not available")
                .eventType(RestartEventType.PRIMARY_NOT_AVAILABLE)
                .build());
            return restartPodOfPrimaryInstance(clusterRestartState, em)
                .chain(() -> restartPodOfReplicas(clusterRestartState, em));
          }
          em.accept(ImmutableRestartEventInstance.builder()
              .message(String.format("Primary instance available in Pod %s",
                  clusterRestartState.getPrimaryInstance().get()))
              .eventType(RestartEventType.PRIMARY_AVAILABLE)
              .build());
          return restartPostgres(clusterRestartState, em)
              .chain(() -> increaseClusterInstance(clusterRestartState, em))
              .chain(() -> restartPodOfReplicas(clusterRestartState, em))
              .chain(() -> performSwitchover(
                  clusterRestartState, em))
              .chain(() -> restartPodOfPrimaryInstance(clusterRestartState, em))
              .chain(() -> decreaseClusterInstance(clusterRestartState, em));
        });
  }

  private Uni<?> restartPostgres(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    if (clusterRestartState.getRestartedInstances().isEmpty()
        && clusterRestartState.getInitialInstances().stream()
        .filter(pod -> clusterRestartState.getPrimaryInstance()
            .map(pod.getMetadata().getName()::equals)
            .orElse(false))
        .anyMatch(clusterRestartState::hasToBeRestarted)) {
      return Uni.createFrom().voidItem()
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Restarting postgres of primary instance in Pod %s of cluster %s",
                    clusterRestartState.getPrimaryInstance().get(),
                    clusterRestartState.getClusterName()))
                .eventType(RestartEventType.RESTARTING_POSTGRES)
                .build());
          })
          .chain(ignored -> postgresRestart.restartPostgres(
              clusterRestartState.getPrimaryInstance().get(),
              clusterRestartState.getClusterName(),
              clusterRestartState.getNamespace()))
          .onItemOrFailure()
          .invoke((restarted, failure) -> checkPostgresRestart(clusterRestartState, em, failure))
          .onFailure()
          .transform(failure -> new FailedRestartPostgresException(
              String.format("Restart of primary instance in Pod %s failed",
                  clusterRestartState.getPrimaryInstance().get()),
                  failure))
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().voidItem();
  }

  private void checkPostgresRestart(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em,
      Throwable failure) {
    if (failure == null) {
      em.accept(ImmutableRestartEventInstance.builder()
          .message(String.format("Restart of primary instance in Pod %s completed",
              clusterRestartState.getPrimaryInstance().get()))
          .eventType(RestartEventType.POSTGRES_RESTARTED)
          .build());
    } else {
      em.accept(ImmutableRestartEventInstance.builder()
          .message(String.format("Restart of primary instance in Pod %s failed: %s",
              clusterRestartState.getPrimaryInstance().get(),
              failure.getMessage()))
          .eventType(RestartEventType.POSTGRES_RESTART_FAILED)
          .build());
    }
  }

  private Uni<?> increaseClusterInstance(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    if (isReducedImpact(clusterRestartState)
        && hasInstancesNotBeenIncreased(clusterRestartState)) {
      return Uni.createFrom().voidItem()
          .onItem()
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Increasing instances"))
                .eventType(RestartEventType.INCREASING_INSTANCES)
                .build());
          })
          .chain(() -> clusterInstanceManager.increaseClusterInstances(
              clusterRestartState.getClusterName(),
              clusterRestartState.getNamespace()))
          .onItem()
          .invoke((createdPod) -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Instances of cluster increased, Pod %s created",
                    createdPod.getMetadata().getName()))
                .pod(createdPod)
                .eventType(RestartEventType.INSTANCES_INCREASED)
                .build());
          })
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().voidItem();
  }

  private Uni<?> restartPodOfReplicas(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    List<Pod> replicas = clusterRestartState.getInitialInstances().stream()
        .filter(pod -> !clusterRestartState.getPrimaryInstance()
            .map(pod.getMetadata().getName()::equals)
            .orElse(false))
        .filter(clusterRestartState::hasToBeRestarted)
        .collect(Collectors.toUnmodifiableList());

    var restartReplicas = Uni.createFrom().nullItem();
    for (Pod replica : replicas) {
      restartReplicas = restartReplicas
          .onItem()
          .invoke(() -> logPodRestartReason(replica, clusterRestartState))
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Restarting replica pod %s",
                    replica.getMetadata().getName()))
                .eventType(RestartEventType.RESTARTING_POD)
                .build());
          })
          .chain(() -> podRestart.restartPod(clusterRestartState.getClusterName(), replica))
          .onItem()
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Pod %s restarted", replica.getMetadata().getName()))
                .pod(replica)
                .eventType(RestartEventType.POD_RESTARTED)
                .build());
          })
          .onFailure()
          .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
              "restarting replica {}", replica.getMetadata().getName()))
          .onFailure()
          .retry()
          .indefinitely()
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return restartReplicas;
  }

  private Uni<?> performSwitchover(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    if (!clusterRestartState.isSwitchoverFinalized()
        && clusterRestartState.getInitialInstances().stream()
        .filter(pod -> clusterRestartState.getPrimaryInstance()
            .map(pod.getMetadata().getName()::equals)
            .orElse(false))
        .anyMatch(clusterRestartState::hasToBeRestarted)) {
      return Uni.createFrom().nullItem()
          .onItem()
          .invoke(() -> em.accept(ImmutableRestartEventInstance.builder()
              .message(String.format("Performing switchover from Pod %s",
                  clusterRestartState.getPrimaryInstance().get()))
              .eventType(RestartEventType.SWITCHOVER_INITIATED)
              .build()))
          .chain(() -> switchoverHandler.performSwitchover(
              clusterRestartState.getPrimaryInstance().get(),
              clusterRestartState.getClusterName(), clusterRestartState.getNamespace()))
          .onItem()
          .invoke(() -> em.accept(ImmutableRestartEventInstance.builder()
              .message(String.format("Switchover performed from Pod %s",
                  clusterRestartState.getPrimaryInstance().get()))
              .eventType(RestartEventType.SWITCHOVER_FINALIZED)
              .build()))
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().nullItem();
  }

  private Uni<?> restartPodOfPrimaryInstance(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    Optional<Pod> primaryPod = clusterRestartState.getInitialInstances().stream()
        .filter(pod -> clusterRestartState.getPrimaryInstance()
            .map(pod.getMetadata().getName()::equals)
            .orElse(false))
        .filter(clusterRestartState::hasToBeRestarted)
        .findAny();
    if (primaryPod.isPresent()) {
      return Uni.createFrom().voidItem()
          .onItem()
          .invoke(() -> logPodRestartReason(
              primaryPod.get(), clusterRestartState))
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Restarting primary Pod %s",
                    clusterRestartState.getPrimaryInstance().get()))
                .eventType(RestartEventType.RESTARTING_POD)
                .build());
          })
          .chain(() -> podRestart.restartPod(
              clusterRestartState.getClusterName(), primaryPod.get()))
          .onItem()
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Pod %s restarted",
                    clusterRestartState.getPrimaryInstance().get()))
                .pod(primaryPod.get())
                .eventType(RestartEventType.POD_RESTARTED)
                .build());
          })
          .chain(() -> waitForClusterToBeHealthy(clusterRestartState));
    }
    return Uni.createFrom().voidItem();
  }

  private Uni<?> decreaseClusterInstance(
      ClusterRestartState clusterRestartState,
      Consumer<RestartEvent> em) {
    if (isReducedImpact(clusterRestartState)
        && hasInstancesNotBeenDecreased(clusterRestartState)) {
      return Uni.createFrom().voidItem()
          .onItem()
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Decreasing instances"))
                .eventType(RestartEventType.DECREASING_INSTANCES)
                .build());
          })
          .chain(() -> clusterInstanceManager.decreaseClusterInstances(
              clusterRestartState.getClusterName(),
              clusterRestartState.getNamespace()))
          .onItem()
          .invoke(() -> {
            em.accept(ImmutableRestartEventInstance.builder()
                .message(String.format("Instances decreased"))
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
