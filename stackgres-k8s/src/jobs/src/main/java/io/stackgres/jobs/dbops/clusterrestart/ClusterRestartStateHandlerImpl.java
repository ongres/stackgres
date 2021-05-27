/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static io.stackgres.jobs.dbops.clusterrestart.ClusterRestartImpl.REDUCED_IMPACT_METHOD;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.jobs.dbops.ClusterRestartStateHandler;
import io.stackgres.jobs.dbops.IllegalDbOpsState;
import io.stackgres.jobs.dbops.StateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StateHandler("restart")
public class ClusterRestartStateHandlerImpl implements ClusterRestartStateHandler {

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

  @Override
  public Uni<StackGresDbOps> restartCluster(StackGresDbOps op) {

    String namespace = op.getMetadata().getNamespace();
    String dbOpName = op.getMetadata().getName();

    StackGresCluster cluster = clusterFinder
        .findByNameAndNamespace(op.getSpec().getSgCluster(), namespace)
        .orElseThrow();

    final Map<String, String> podLabels = labelFactory.patroniClusterLabels(cluster);

    List<Pod> clusterPods = podScanner.findByLabelsAndNamespace(namespace, podLabels);

    StackGresDbOps initializedOp = initRestart(op, clusterPods);

    ClusterRestartState clusterRestartState = buildClusterState(initializedOp, clusterPods);

    return Uni.createFrom().emitter(em -> {
      Multi<RestartEvent> restartMulti = clusterRestart.restartCluster(clusterRestartState);

      restartMulti
          .onItem()
          .transform(event -> {
            var updatedDbOps = updateJobStatus(dbOpName, namespace, event);
            return Tuple2.of(updatedDbOps, event);
          }).subscribe()
          .with(
              tuple -> logEvent(tuple.getItem1(), tuple.getItem2()),
              error -> {
                reportFailure(initializedOp, error);
                em.fail(error);
              },
              () -> {
                var lastDbOps = getDbOp(dbOpName, namespace);
                em.complete(lastDbOps);
              }
          );

    });
  }

  private StackGresDbOps getDbOp(String name, String namespace) {
    return dbOpsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow();
  }

  private void reportFailure(StackGresDbOps initializedOp, Throwable error) {
    LOGGER.error("Unexpected error on restarting cluster " + initializedOp.getSpec().getSgCluster(),
        error);
  }

  private void logEvent(StackGresDbOps initializedOp, RestartEvent event) {
    switch (event.getEventType()) {
      case POD_CREATED:
        LOGGER.info("Pod {} created", event.getPod().getMetadata().getName());
        break;
      case SWITCHOVER:
        LOGGER.info("Switchover of cluster " + initializedOp.getSpec().getSgCluster()
            + " performed");
        break;
      default:
        LOGGER.info("Pod {} restarted", event.getPod().getMetadata().getName());
    }
  }

  private StackGresDbOps updateJobStatus(String dbOpName,
                                         String namespace,
                                         RestartEvent restartEvent) {

    var refreshedOp = getDbOp(dbOpName, namespace);

    final StackGresDbOpsRestartStatus restartStatus = refreshedOp.getStatus()
        .getRestart();
    final String podName = restartEvent.getPod().getMetadata().getName();
    switch (restartEvent.getEventType()) {
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
        break;
    }
    return dbOpsScheduler.update(refreshedOp);
  }

  private StackGresDbOps initRestart(StackGresDbOps op, List<Pod> clusterPods) {
    if (op.getStatus() == null || op.getStatus().getOpStarted() == null) {
      throw new IllegalDbOpsState(StackGresDbOps.KIND + " "
          + op.getMetadata().getName() + " not initialized");
    }

    if (op.getStatus().getRestart() == null) {
      var restartStatus = new StackGresDbOpsRestartStatus();

      List<ObjectMeta> clusterPodsMeta = clusterPods.stream()
          .map(Pod::getMetadata)
          .collect(Collectors.toUnmodifiableList());

      List<String> initialInstances = clusterPodsMeta.stream().map(ObjectMeta::getName)
          .sorted(String::compareTo)
          .collect(Collectors.toUnmodifiableList());

      restartStatus.setPrimaryInstance(clusterPodsMeta.stream()
          .filter(pm -> StackGresContext.PRIMARY_ROLE.equals(pm.getLabels()
              .get(StackGresContext.ROLE_KEY)))
          .findFirst().orElseThrow(() -> new InvalidCluster("Cluster has no primary pod"))
          .getName());

      restartStatus.setInitialInstances(initialInstances);
      restartStatus.setPendingToRestartInstances(initialInstances);
      restartStatus.setSwitchoverInitiated(Boolean.FALSE.toString());
      op.getStatus().setRestart(restartStatus);

      return dbOpsScheduler.update(op);
    }
    return op;
  }

  protected ClusterRestartState buildClusterState(StackGresDbOps op, List<Pod> clusterPods) {
    Map<String, Pod> podsDict = clusterPods.stream()
        .collect(Collectors.toMap(pod -> pod.getMetadata().getName(), Function.identity()));

    final var restartStatus = op.getStatus().getRestart();
    var initialInstances = Optional.ofNullable(restartStatus.getInitialInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .collect(Collectors.toUnmodifiableList()))
        .orElse(List.of());

    var restartedInstances = Optional.ofNullable(restartStatus.getRestartedInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .collect(Collectors.toUnmodifiableList()))
        .orElse(List.of());

    final String method = Optional.of(op.getSpec())
        .map(StackGresDbOpsSpec::getRestart)
        .map(StackGresDbOpsRestart::getMethod)
        .orElse(REDUCED_IMPACT_METHOD);

    return ImmutableClusterRestartState.builder()
        .restartMethod(method)
        .clusterName(op.getSpec().getSgCluster())
        .namespace(op.getMetadata().getNamespace())
        .primaryInstance(clusterPods.stream()
            .filter(pod -> StackGresContext.PRIMARY_ROLE.equals(
                pod.getMetadata().getLabels().get(StackGresContext.ROLE_KEY)))
            .findFirst().orElseThrow(() -> new InvalidCluster("Cluster has no primary pod")))
        .isSwitchoverInitiated(Boolean.parseBoolean(restartStatus.getSwitchoverInitiated()))
        .initialInstances(initialInstances)
        .restartedInstances(restartedInstances)
        .totalInstances(clusterPods)
        .build();

  }
}

