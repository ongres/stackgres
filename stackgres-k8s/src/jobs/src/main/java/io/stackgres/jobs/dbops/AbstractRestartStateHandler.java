/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import static io.stackgres.jobs.dbops.clusterrestart.ClusterRestartImpl.REDUCED_IMPACT_METHOD;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestart;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartStateHandlerImpl;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.InvalidCluster;
import io.stackgres.jobs.dbops.clusterrestart.RestartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRestartStateHandler implements ClusterRestartStateHandler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterRestartStateHandlerImpl.class);

  @Inject
  ClusterRestart clusterRestart;

  @Inject
  CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  LabelFactory<StackGresCluster> labelFactory;

  @Inject
  ResourceScanner<Pod> podScanner;

  @Inject
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  @Override
  public Uni<StackGresDbOps> restartCluster(StackGresDbOps op) {

    String clusterName = op.getSpec().getSgCluster();
    String dbOpsName = op.getMetadata().getName();
    String namespace = op.getMetadata().getNamespace();

    return initClusterDbOpsStatus(clusterName, namespace)
        .chain(tuple -> initDbOpsStatus(dbOpsName, namespace, tuple.getItem2()))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10)
        .onItem()
        .transform(tuple -> buildClusterRestartState(tuple.getItem1(), tuple.getItem2()))
        .chain(state -> restartCluster(state, dbOpsName));
  }

  private Uni<StackGresDbOps> restartCluster(ClusterRestartState state, String dbOpName) {
    return Uni.createFrom().emitter(em -> {
      Multi<RestartEvent> restartMulti = clusterRestart.restartCluster(state);

      String namespace = state.getNamespace();

      restartMulti
          .onItem()
          .call(event -> updateJobStatus(dbOpName, namespace, event))
          .subscribe()
          .with(
              event -> logEvent(state.getClusterName(), event),
              error -> {
                reportFailure(state.getClusterName(), error);
                em.fail(error);
              },
              () ->
                  findSgCluster(state.getClusterName(), namespace)
                      .chain(this::cleanCluster)
                      .chain(() -> findDbOps(dbOpName, namespace))
                      .subscribe()
                      .with(em::complete)
          );
    });
  }

  protected abstract void cleanClusterStatus(StackGresCluster cluster);

  protected Uni<StackGresDbOps> updateJobStatus(String dbOpName,
                                                String namespace,
                                                RestartEvent event) {
    return findDbOps(dbOpName, namespace)
        .chain(dbOps -> updateJobStatus(dbOps, event))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10);
  }

  private Uni<StackGresDbOps> updateJobStatus(StackGresDbOps dbOps, RestartEvent event) {

    return Uni.createFrom().emitter(em -> {

      var restartStatus = getDbOpRestartStatus(dbOps);

      var podName = event.getPod().getMetadata().getName();

      switch (event.getEventType()) {
        case POD_RESTART:
          List<String> pendingInstances = restartStatus.getPendingToRestartInstances();
          pendingInstances.remove(podName);
          if (restartStatus.getRestartedInstances() == null) {
            restartStatus.setRestartedInstances(new ArrayList<>());
          }
          restartStatus.getRestartedInstances().add(podName);
          break;
        case POD_CREATED:
          if (restartStatus.getRestartedInstances() == null) {
            restartStatus.setRestartedInstances(new ArrayList<>());
          }
          restartStatus.getRestartedInstances().add(podName);
          break;
        default:
          restartStatus.setSwitchoverInitiated(Boolean.TRUE.toString());
      }

      var newStatus = dbOpsScheduler.update(dbOps);
      em.complete(newStatus);
    });
  }

  protected abstract boolean isSgClusterDbOpsStatusInitialized(StackGresCluster cluster);

  protected abstract boolean isDbOpsStatusInitialized(StackGresDbOps cluster);

  protected Uni<List<Pod>> scanClusterPods(StackGresCluster cluster) {
    return Uni.createFrom().emitter(em -> {

      String namespace = cluster.getMetadata().getNamespace();

      final Map<String, String> podLabels = labelFactory.patroniClusterLabels(cluster);

      List<Pod> clusterPods = podScanner.findByLabelsAndNamespace(namespace, podLabels);

      em.complete(clusterPods);
    });
  }

  public Uni<Tuple2<StackGresCluster, List<Pod>>> initClusterDbOpsStatus(
      String clusterName, String namespace) {

    return findSgCluster(clusterName, namespace)
        .chain(cluster -> scanClusterPods(cluster)
            .onItem().transform(pods -> Tuple2.of(cluster, pods)))
        .onItem().transform(tuple -> {
          final StackGresCluster cluster = tuple.getItem1();
          final List<Pod> pods = tuple.getItem2();
          if (isSgClusterDbOpsStatusInitialized(cluster)) {
            return Tuple2.of(cluster, pods);
          } else {
            var clusterStatus = getClusterRestartStatus(cluster);
            initRestartStatusValues(clusterStatus, pods);
            var newCluster = clusterScheduler.updateStatus(cluster);
            return Tuple2.of(newCluster, pods);
          }
        });

  }

  public Uni<Tuple2<StackGresDbOps, List<Pod>>> initDbOpsStatus(String dbOpsName,
                                                                String namespace,
                                                                List<Pod> pods) {

    return findDbOps(dbOpsName, namespace)
        .onItem()
        .transform(dbOps -> {
          if (isDbOpsStatusInitialized(dbOps)) {
            return Tuple2.of(dbOps, pods);
          } else {
            var dbOpsRestartStatus = getDbOpRestartStatus(dbOps);
            initDbOpsRestartStatusValues(dbOpsRestartStatus, pods);
            var newDbOps = dbOpsScheduler.update(dbOps);
            return Tuple2.of(newDbOps, pods);
          }
        });

  }

  protected abstract Optional<String> getRestartMethod(StackGresDbOps op);

  private void logEvent(String clusterName, RestartEvent event) {
    switch (event.getEventType()) {
      case POD_CREATED:
        LOGGER.info("Pod {} created", event.getPod().getMetadata().getName());
        break;
      case SWITCHOVER:
        LOGGER.info("Switchover of cluster {} performed", clusterName);
        break;
      default:
        LOGGER.info("Pod {} restarted", event.getPod().getMetadata().getName());
    }
  }

  protected void reportFailure(String clusterName, Throwable error) {
    LOGGER.error("Unexpected error on restarting cluster {}", clusterName, error);
  }

  protected abstract DbOpsRestartStatus getDbOpRestartStatus(StackGresDbOps dbOps);

  protected abstract ClusterDbOpsRestartStatus getClusterRestartStatus(StackGresCluster dbOps);

  public ClusterRestartState buildClusterRestartState(StackGresDbOps dbOp, List<Pod> pods) {

    DbOpsRestartStatus restartStatus = getDbOpRestartStatus(dbOp);
    Map<String, Pod> podsDict = pods.stream()
        .collect(Collectors.toMap(pod -> pod.getMetadata().getName(), Function.identity()));

    var initialInstances = Optional.ofNullable(restartStatus.getInitialInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .collect(Collectors.toUnmodifiableList()))
        .orElse(List.of());

    var restartedInstances = Optional.ofNullable(restartStatus.getRestartedInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .collect(Collectors.toUnmodifiableList()))
        .orElse(List.of());

    final String method = getRestartMethod(dbOp)
        .orElse(REDUCED_IMPACT_METHOD);

    return ImmutableClusterRestartState.builder()
        .restartMethod(method)
        .clusterName(dbOp.getSpec().getSgCluster())
        .namespace(dbOp.getMetadata().getNamespace())
        .primaryInstance(getPrimaryInstance(pods))
        .isSwitchoverInitiated(Boolean.parseBoolean(restartStatus.getSwitchoverInitiated()))
        .initialInstances(initialInstances)
        .restartedInstances(restartedInstances)
        .totalInstances(pods)
        .build();
  }

  private Pod getPrimaryInstance(List<Pod> pods) {
    return pods.stream()
        .filter(pod -> StackGresContext.PRIMARY_ROLE.equals(
            pod.getMetadata().getLabels().get(StackGresContext.ROLE_KEY)))
        .findFirst().orElseThrow(() -> new InvalidCluster("Cluster has no primary pod"));
  }

  protected void initRestartStatusValues(ClusterDbOpsRestartStatus restartStatus, List<Pod> pods) {
    String primaryInstance = getPrimaryInstance(pods)
        .getMetadata().getName();

    List<String> instances = pods.stream()
        .map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .collect(Collectors.toList());
    restartStatus.setInitialInstances(instances);
    restartStatus.setPrimaryInstance(primaryInstance);
  }

  protected void initDbOpsRestartStatusValues(DbOpsRestartStatus restartStatus, List<Pod> pods) {
    String primaryInstance = getPrimaryInstance(pods)
        .getMetadata().getName();

    List<String> instances = pods.stream()
        .map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .collect(Collectors.toList());
    restartStatus.setInitialInstances(instances);
    restartStatus.setPendingToRestartInstances(instances);
    restartStatus.setPrimaryInstance(primaryInstance);
  }

  protected Uni<StackGresCluster> findSgCluster(String name, String namespace) {
    return Uni.createFrom().emitter(em ->
        clusterFinder.findByNameAndNamespace(name, namespace).ifPresentOrElse(
            em::complete,
            () -> em.fail(new IllegalArgumentException("SGCluster " + name + " not found"))
        ));
  }

  protected Uni<StackGresCluster> cleanCluster(StackGresCluster cluster) {
    return Uni.createFrom().emitter(em -> {
      cleanClusterStatus(cluster);
      var updatedCluster = clusterScheduler.updateStatus(cluster);
      em.complete(updatedCluster);
    });
  }

  protected Uni<StackGresDbOps> findDbOps(String name, String namespace) {
    return Uni.createFrom().emitter(em ->
        dbOpsFinder.findByNameAndNamespace(name, namespace).ifPresentOrElse(
            em::complete,
            () -> em.fail(new IllegalArgumentException("SGDbOps " + name + " not found"))
        )
    );
  }
}
