/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterPendingRestartUtil;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestart;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.InvalidClusterException;
import io.stackgres.jobs.dbops.clusterrestart.RestartEvent;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRestartStateHandler implements ClusterRestartStateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRestartStateHandler.class);

  @Inject
  ClusterRestart clusterRestart;

  @Inject
  CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  ResourceFinder<StatefulSet> statefulSetFinder;

  @Inject
  ResourceScanner<Pod> podScanner;

  @Inject
  PatroniCtl patroniCtl;

  @Inject
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  @Inject
  EventEmitter<StackGresDbOps> eventEmitter;

  @Inject
  ObjectMapper objectMapper;

  @Inject
  DbOpsExecutorService executorService;

  @Override
  public Uni<ClusterRestartState> restartCluster(StackGresDbOps dbOps) {
    String clusterName = dbOps.getSpec().getSgCluster();
    String dbOpsName = dbOps.getMetadata().getName();
    String namespace = dbOps.getMetadata().getNamespace();

    return getClusterRestartState(namespace, dbOpsName, clusterName)
        .call(this::initClusterDbOpsStatus)
        .call(clusterRestartState -> initDbOpsStatus(clusterRestartState, dbOps))
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("asserting the operation status"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10)
        .chain(this::restartCluster);
  }

  private Uni<ClusterRestartState> restartCluster(ClusterRestartState clusterRestartState) {
    return Uni.createFrom().voidItem()
        .emitOn(executorService.getExecutorService())
        .chain(() -> clusterRestart.restartCluster(clusterRestartState)
          .onItem()
          .call(event -> updateDbOpsStatus(event, clusterRestartState))
          .onItem()
          .call(event -> recordEvent(event, clusterRestartState))
          .onItem()
          .invoke(this::logEvent)
          .onFailure()
          .call(error -> reportFailure(clusterRestartState.getClusterName(), error))
          .collect()
          .last())
        .call(() -> findSgCluster(clusterRestartState.getClusterName(),
            clusterRestartState.getNamespace())
            .chain(this::cleanCluster)
            .onFailure()
            .transform(MutinyUtil.logOnFailureToRetry("cleaning cluster status"))
            .onFailure()
            .retry()
            .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
            .indefinitely())
        .chain(ignore -> Uni.createFrom().item(clusterRestartState));
  }

  protected abstract void cleanClusterStatus(StackGresCluster cluster);

  protected Uni<StackGresDbOps> updateDbOpsStatus(RestartEvent event,
      ClusterRestartState clusterRestartState) {
    return findDbOps(clusterRestartState.getDbOpsName(), clusterRestartState.getNamespace())
        .chain(dbOps -> updateDbOpsStatus(dbOps, event))
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("updating SGDbOps status"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10);
  }

  private Uni<StackGresDbOps> updateDbOpsStatus(StackGresDbOps dbOps, RestartEvent event) {
    return Uni.createFrom().item(() -> {
      var restartStatus = getDbOpRestartStatus(dbOps);

      var podNameOpt = event.getPod().map(Pod::getMetadata)
          .map(ObjectMeta::getName);

      switch (event.getEventType()) {
        case SWITCHOVER_INITIATED:
          restartStatus.setSwitchoverInitiated(Instant.now().toString());
          break;
        case SWITCHOVER_FINALIZED:
          restartStatus.setSwitchoverFinalized(Instant.now().toString());
          break;
        case POD_RESTARTED:
          List<String> pendingInstances = restartStatus.getPendingToRestartInstances();
          var podName = podNameOpt.orElseThrow();
          pendingInstances.remove(podName);
          if (restartStatus.getRestartedInstances() == null) {
            restartStatus.setRestartedInstances(new ArrayList<>());
          }
          restartStatus.getRestartedInstances().add(podName);
          break;
        case INSTANCES_INCREASED:
          if (restartStatus.getRestartedInstances() == null) {
            restartStatus.setRestartedInstances(new ArrayList<>());
          }
          restartStatus.getRestartedInstances().add(podNameOpt.orElseThrow());
          break;
        default:
          break;
      }

      setDbOpRestartStatus(dbOps, restartStatus);
      return dbOps;
    })
        .chain(() -> executorService.itemAsync(() -> dbOpsScheduler.update(dbOps)));
  }

  protected abstract boolean isSgClusterDbOpsStatusInitialized(StackGresCluster cluster);

  protected abstract boolean isDbOpsStatusInitialized(StackGresDbOps cluster);

  protected Uni<List<Pod>> scanClusterPods(StackGresCluster cluster) {
    return executorService.itemAsync(() -> {
      String namespace = cluster.getMetadata().getNamespace();
      final Map<String, String> podLabels =
          labelFactory.clusterLabelsWithoutUidAndScope(cluster);
      List<Pod> clusterPods = podScanner.findByLabelsAndNamespace(namespace, podLabels);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Retrieved cluster pods with labels {}: {}",
            podLabels.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(",")),
            clusterPods.stream()
            .map(HasMetadata::getMetadata)
            .map(ObjectMeta::getName)
            .collect(Collectors.joining(" ")));
        List<Pod> allPods = podScanner.findResourcesInNamespace(namespace);
        LOGGER.trace("Found pods with labels: {}",
            allPods.stream()
            .map(HasMetadata::getMetadata)
            .map(metadata -> metadata.getName() + ":"
                + Optional.ofNullable(metadata.getLabels())
                .map(Map::entrySet)
                .stream()
                .flatMap(Set::stream)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")))
            .collect(Collectors.joining(" ")));
      }
      return clusterPods;
    });
  }

  protected Uni<ClusterRestartState> getClusterRestartState(
      String namespace, String dbOpsName, String clusterName) {
    return Uni.combine().all().unis(
            findDbOps(dbOpsName, namespace),
            findSgCluster(clusterName, namespace)
                .chain(cluster -> Uni.combine().all().unis(
                        Uni.createFrom().item(cluster),
                        getClusterStatefulSet(cluster),
                        scanClusterPods(cluster))
                    .asTuple()))
        .asTuple()
        .onItem()
        .transform(tuple -> buildClusterRestartState(
            tuple.getItem1(), tuple.getItem2().getItem1(),
            tuple.getItem2().getItem2(), tuple.getItem2().getItem3()));
  }

  protected Uni<?> initClusterDbOpsStatus(ClusterRestartState clusterRestartState) {
    return Uni.combine().all().unis(
        findDbOps(clusterRestartState.getDbOpsName(), clusterRestartState.getNamespace()),
        findSgCluster(clusterRestartState.getClusterName(), clusterRestartState.getNamespace()))
        .asTuple()
        .chain(tuple -> {
          if (isSgClusterDbOpsStatusInitialized(tuple.getItem2())) {
            return Uni.createFrom().voidItem();
          } else {
            return initClusterDbOpsStatusValues(
                clusterRestartState, tuple.getItem1(), tuple.getItem2())
                .chain(() -> executorService.itemAsync(
                    () -> clusterScheduler.update(tuple.getItem2(),
                        (currentCluster) -> {
                          var dbOpsStatus = Optional.ofNullable(tuple.getItem2().getStatus())
                              .map(StackGresClusterStatus::getDbOps)
                              .orElse(null);
                          if (currentCluster.getStatus() == null) {
                            currentCluster.setStatus(new StackGresClusterStatus());
                          }
                          currentCluster.getStatus().setDbOps(dbOpsStatus);
                        })));
          }
        });
  }

  protected Uni<?> initDbOpsStatus(ClusterRestartState clusterRestartState, StackGresDbOps dbOps) {
    if (isDbOpsStatusInitialized(dbOps)) {
      return Uni.createFrom().voidItem();
    } else {
      return findSgCluster(clusterRestartState.getClusterName(), clusterRestartState.getNamespace())
          .chain(cluster -> {
            return initDbOpsRestartStatusValues(clusterRestartState, dbOps, cluster)
                .chain(() -> executorService.itemAsync(() -> dbOpsScheduler.update(dbOps)));
          });
    }
  }

  protected Uni<Void> initDbOpsRestartStatusValues(ClusterRestartState clusterRestartState,
      StackGresDbOps dbOps, StackGresCluster cluster) {
    var restartStatus = getDbOpRestartStatus(dbOps);

    restartStatus.setInitialInstances(
        clusterRestartState.getInitialInstances()
            .stream()
            .map(Pod::getMetadata)
            .map(ObjectMeta::getName)
            .sorted(String::compareTo)
            .collect(Collectors.toList()));
    restartStatus.setPendingToRestartInstances(
        clusterRestartState.getInitialInstances()
            .stream()
            .filter(clusterRestartState::hasToBeRestarted)
            .map(Pod::getMetadata)
            .map(ObjectMeta::getName)
            .sorted(String::compareTo)
            .collect(Collectors.toList()));
    restartStatus.setPrimaryInstance(
        clusterRestartState.getPrimaryInstance()
            .getMetadata().getName());
    return Uni.createFrom().voidItem();
  }

  private @NotNull Uni<Optional<StatefulSet>> getClusterStatefulSet(StackGresCluster cluster) {
    return executorService.itemAsync(
        () -> statefulSetFinder.findByNameAndNamespace(
            cluster.getMetadata().getName(), cluster.getMetadata().getNamespace()));
  }

  protected abstract Optional<DbOpsMethodType> getRestartMethod(StackGresDbOps op);

  private void logEvent(RestartEvent event) {
    LOGGER.info(event.getMessage());
  }

  protected Uni<Void> reportFailure(String clusterName, Throwable error) {
    LOGGER.error("Unexpected error on restarting cluster {}", clusterName, error);
    return Uni.createFrom().voidItem();
  }

  protected abstract DbOpsRestartStatus getDbOpRestartStatus(StackGresDbOps dbOps);

  protected abstract void setDbOpRestartStatus(StackGresDbOps dbOps,
                                               DbOpsRestartStatus dbOpsStatus);

  protected abstract ClusterDbOpsRestartStatus getClusterRestartStatus(StackGresCluster cluster);

  protected ClusterRestartState buildClusterRestartState(StackGresDbOps dbOps,
      StackGresCluster cluster, Optional<StatefulSet> statefulSet, List<Pod> clusterPods) {
    final DbOpsOperation operation = DbOpsOperation.fromString(dbOps.getSpec().getOp());
    final DbOpsMethodType method = getRestartMethod(dbOps)
        .orElse(DbOpsMethodType.REDUCED_IMPACT);
    final boolean onlyPendingRestart = Optional.of(dbOps.getSpec())
        .map(StackGresDbOpsSpec::getRestart)
        .map(StackGresDbOpsRestart::getOnlyPendingRestart)
        .orElse(false);
    final DbOpsRestartStatus restartStatus = getDbOpRestartStatus(dbOps);
    final Map<String, Pod> podsDict = clusterPods.stream()
        .collect(Collectors.toMap(pod -> pod.getMetadata().getName(), Function.identity()));
    final Pod primaryInstance = getPrimaryInstance(clusterPods, cluster);
    final var initialInstances = Optional.ofNullable(restartStatus.getInitialInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .toList())
        .orElse(clusterPods);
    final var restartedInstances = Optional.ofNullable(restartStatus.getRestartedInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .toList())
        .orElse(List.of());
    final var podRestartReasonsMap = clusterPods.stream()
        .collect(Collectors.toUnmodifiableMap(
            Function.identity(),
            pod -> getPodRestartReasons(cluster, statefulSet, pod)));

    LOGGER.info("Operation: {}", operation.toString());
    LOGGER.info("Restart method: {}", method.toString());
    LOGGER.info("Only pending restart: {}", onlyPendingRestart);
    LOGGER.info("Found cluster pods: {}", clusterPods.stream()
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .collect(Collectors.joining(" ")));
    LOGGER.info("Primary instance: {}", primaryInstance.getMetadata().getName());
    LOGGER.info("Initial pods: {}", initialInstances.stream()
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .collect(Collectors.joining(" ")));
    LOGGER.info("Already restarted pods: {}", restartedInstances.stream()
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .collect(Collectors.joining(" ")));
    LOGGER.info("Restart reasons: {}", podRestartReasonsMap.entrySet().stream()
        .map(e -> e.getKey().getMetadata().getName() + ":" + e.getValue().getReasons()
            .stream().map(Enum::name).collect(Collectors.joining(",")))
        .collect(Collectors.joining(" ")));
    LOGGER.info("Switchover initialized: {}", Optional.of(restartStatus)
        .map(DbOpsRestartStatus::getSwitchoverInitiated)
        .orElse("no"));
    LOGGER.info("Switchover finalized: {}", Optional.of(restartStatus)
        .map(DbOpsRestartStatus::getSwitchoverFinalized)
        .orElse("no"));

    return ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(operation)
        .clusterName(cluster.getMetadata().getName())
        .restartMethod(method)
        .isOnlyPendingRestart(onlyPendingRestart)
        .primaryInstance(primaryInstance)
        .isSwitchoverInitiated(restartStatus.getSwitchoverInitiated() != null)
        .isSwitchoverFinalized(restartStatus.getSwitchoverFinalized() != null)
        .initialInstances(initialInstances)
        .restartedInstances(restartedInstances)
        .totalInstances(clusterPods)
        .podRestartReasonsMap(podRestartReasonsMap)
        .build();
  }

  private RestartReasons getPodRestartReasons(StackGresCluster cluster,
                                              Optional<StatefulSet> statefulSet, Pod pod) {
    return ClusterPendingRestartUtil.getRestartReasons(
        Optional.ofNullable(cluster.getStatus())
            .map(StackGresClusterStatus::getPodStatuses)
            .orElse(ImmutableList.of()),
        statefulSet,
        ImmutableList.of(pod));
  }

  protected Pod getPrimaryInstance(List<Pod> pods, StackGresCluster cluster) {
    Integer latestPrimaryIndex = PatroniUtil.getLatestPrimaryIndexFromPatroni(
        patroniCtl.instanceFor(cluster));
    return pods.stream()
        .filter(pod -> PatroniUtil.PRIMARY_ROLE.equals(
            pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)))
        .findFirst()
        .or(() -> pods.stream()
            .filter(pod -> ResourceUtil.getIndexPattern()
                .matcher(pod.getMetadata().getName()).results()
                .findFirst()
                .map(result -> result.group(1))
                .map(Integer::parseInt)
                .filter(latestPrimaryIndex::equals)
                .isPresent())
            .findFirst())
        .orElseThrow(() -> new InvalidClusterException("Cluster has no primary pod"));
  }

  protected Uni<Void> initClusterDbOpsStatusValues(ClusterRestartState clusterRestartState,
      StackGresDbOps dbOps, StackGresCluster cluster) {
    var restartStatus = getClusterRestartStatus(cluster);

    restartStatus.setInitialInstances(
        clusterRestartState.getInitialInstances()
            .stream()
            .map(Pod::getMetadata)
            .map(ObjectMeta::getName)
            .sorted(String::compareTo)
            .collect(Collectors.toList()));
    restartStatus.setPrimaryInstance(clusterRestartState.getPrimaryInstance()
        .getMetadata().getName());
    return Uni.createFrom().voidItem();
  }

  protected Uni<StackGresCluster> findSgCluster(String name, String namespace) {
    return executorService.itemAsync(
        () -> clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + name + " not found")));
  }

  protected Uni<StackGresCluster> cleanCluster(StackGresCluster cluster) {
    return Uni.createFrom().voidItem()
        .invoke(item -> cleanClusterStatus(cluster))
        .chain(() -> executorService.itemAsync(
            () -> clusterScheduler.update(cluster,
                (currentCluster) -> {
                  var dbOps = Optional.ofNullable(cluster.getStatus())
                      .map(StackGresClusterStatus::getDbOps)
                      .orElse(null);
                  if (currentCluster.getStatus() == null) {
                    currentCluster.setStatus(new StackGresClusterStatus());
                  }
                  currentCluster.getStatus().setDbOps(dbOps);
                })));
  }

  protected Uni<StackGresDbOps> findDbOps(String name, String namespace) {
    return executorService.itemAsync(
        () -> dbOpsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDbOps " + name + " not found")));
  }

  protected Uni<?> recordEvent(RestartEvent event, ClusterRestartState restartState) {
    return findDbOps(restartState.getDbOpsName(), restartState.getNamespace())
        .chain(dbOps -> executorService.invokeAsync(
            () -> eventEmitter.sendEvent(
                event.getEventType(), event.getMessage(), dbOps)));
  }

}
