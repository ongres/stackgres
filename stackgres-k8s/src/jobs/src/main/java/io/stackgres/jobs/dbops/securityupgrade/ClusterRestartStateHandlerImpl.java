/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

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
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestart;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.InvalidCluster;
import io.stackgres.jobs.dbops.clusterrestart.RestartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRestartStateHandlerImpl implements ClusterRestartStateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRestartStateHandlerImpl.class);

  private final ClusterRestart clusterRestart;

  private final CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  private final CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final LabelFactory<StackGresCluster> labelFactory;

  private final ResourceScanner<Pod> podScanner;

  @Inject
  public ClusterRestartStateHandlerImpl(ClusterRestart clusterRestart,
                                        CustomResourceScheduler<StackGresDbOps> dbOpsScheduler,
                                        CustomResourceFinder<StackGresDbOps> dbOpsFinder,
                                        CustomResourceFinder<StackGresCluster> clusterFinder,
                                        LabelFactory<StackGresCluster> labelFactory,
                                        ResourceScanner<Pod> podScanner) {
    this.clusterRestart = clusterRestart;
    this.dbOpsScheduler = dbOpsScheduler;
    this.dbOpsFinder = dbOpsFinder;
    this.clusterFinder = clusterFinder;
    this.labelFactory = labelFactory;
    this.podScanner = podScanner;
  }

  @Override
  public Uni<StackGresDbOps> restartCluster(StackGresDbOps op) {

    String namespace = op.getMetadata().getNamespace();
    String dbOpName = op.getMetadata().getName();

    StackGresCluster cluster = clusterFinder
        .findByNameAndNamespace(op.getSpec().getSgCluster(), namespace)
        .orElseThrow();

    final Map<String, String> podLabels = labelFactory.patroniClusterLabels(cluster);

    List<Pod> clusterPods = podScanner.findByLabelsAndNamespace(namespace, podLabels);

    StackGresDbOps initializedOp = initSecurityUpgrade(op, clusterPods);

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

  private void reportFailure(StackGresDbOps initializedOp, Throwable error) {
    LOGGER.error("Unexpected error on restarting cluster " + initializedOp.getSpec().getSgCluster(),
        error);
  }

  private void logEvent(StackGresDbOps initializedOp, RestartEvent event) {
    switch (event.getEventType()) {
      case POD_RESTART:
        LOGGER.info("Pod {} restarted", event.getPod().getMetadata().getName());
        break;
      case POD_CREATED:
        LOGGER.info("Pod {} created", event.getPod().getMetadata().getName());
        break;
      case SWITCHOVER:
        LOGGER.info("Switchover of cluster " + initializedOp.getSpec().getSgCluster()
            + " performed");
        break;
    }
  }

  private StackGresDbOps getDbOp(String name, String namespace){
    return dbOpsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow();
  }

  private StackGresDbOps updateJobStatus(String dbOpName,
                                         String namespace,
                                         RestartEvent restartEvent) {

    var refreshedOp = getDbOp(dbOpName, namespace);

    final StackGresDbOpsSecurityUpgradeStatus securityUpgradeStatus = refreshedOp.getStatus()
        .getSecurityUpgrade();
    final String podName = restartEvent.getPod().getMetadata().getName();
    switch (restartEvent.getEventType()) {
      case POD_RESTART:
        List<String> pendingInstances = securityUpgradeStatus.getPendingToRestartInstances();
        pendingInstances.remove(podName);
        if (securityUpgradeStatus.getRestartedInstances() == null) {
          securityUpgradeStatus.setRestartedInstances(new ArrayList<>());
        }
        securityUpgradeStatus.getRestartedInstances().add(podName);
        break;
      case POD_CREATED:
        if (securityUpgradeStatus.getRestartedInstances() == null) {
          securityUpgradeStatus.setRestartedInstances(new ArrayList<>());
        }
        securityUpgradeStatus.getRestartedInstances().add(podName);
        break;
      case SWITCHOVER:
        securityUpgradeStatus.setSwitchoverInitiated(Boolean.TRUE.toString());
        break;
    }
    return dbOpsScheduler.update(refreshedOp);
  }

  protected StackGresDbOps initSecurityUpgrade(StackGresDbOps op, List<Pod> clusterPods) {
    if (op.getStatus() == null || op.getStatus().getOpStarted() == null) {
      throw new IllegalDbOpsState(StackGresDbOps.KIND + " "
          + op.getMetadata().getName() + " not initialized");
    }

    if (op.getStatus().getSecurityUpgrade() == null) {
      var securityUpgradeStatus = new StackGresDbOpsSecurityUpgradeStatus();

      List<ObjectMeta> clusterPodsMeta = clusterPods.stream()
          .map(Pod::getMetadata)
          .collect(Collectors.toUnmodifiableList());

      List<String> initialInstances = clusterPodsMeta.stream().map(ObjectMeta::getName)
          .sorted(String::compareTo)
          .collect(Collectors.toUnmodifiableList());

      securityUpgradeStatus.setPrimaryInstance(clusterPodsMeta.stream()
          .filter(pm -> StackGresContext.PRIMARY_ROLE.equals(pm.getLabels()
              .get(StackGresContext.ROLE_KEY)))
          .findFirst().orElseThrow(() -> new InvalidCluster("Cluster has no primary pod"))
          .getName());

      securityUpgradeStatus.setInitialInstances(initialInstances);
      securityUpgradeStatus.setPendingToRestartInstances(initialInstances);
      securityUpgradeStatus.setSwitchoverInitiated(Boolean.FALSE.toString());
      op.getStatus().setSecurityUpgrade(securityUpgradeStatus);

      return dbOpsScheduler.update(op);
    }

    return op;
  }

  protected ClusterRestartState buildClusterState(StackGresDbOps op, List<Pod> clusterPods) {
    Map<String, Pod> podsDict = clusterPods.stream()
        .collect(Collectors.toMap(pod -> pod.getMetadata().getName(), Function.identity()));

    final var securityUpgradeStatus = op.getStatus().getSecurityUpgrade();
    var initialInstances = Optional.ofNullable(securityUpgradeStatus.getInitialInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .collect(Collectors.toUnmodifiableList()))
        .orElse(List.of());

    var restartedInstances = Optional.ofNullable(securityUpgradeStatus.getRestartedInstances())
        .map(instances -> instances.stream().map(podsDict::get)
            .collect(Collectors.toUnmodifiableList()))
        .orElse(List.of());

    return ImmutableClusterRestartState.builder()
        .restartMethod(op.getSpec().getSecurityUpgrade().getMethod())
        .clusterName(op.getSpec().getSgCluster())
        .namespace(op.getMetadata().getNamespace())
        .primaryInstance(clusterPods.stream()
            .filter(pod -> StackGresContext.PRIMARY_ROLE.equals(
                pod.getMetadata().getLabels().get(StackGresContext.ROLE_KEY)))
            .findFirst().orElseThrow(() -> new InvalidCluster("Cluster has no primary pod")))
        .isSwitchoverInitiated(Boolean.parseBoolean(securityUpgradeStatus.getSwitchoverInitiated()))
        .initialInstances(initialInstances)
        .restartedInstances(restartedInstances)
        .totalInstances(clusterPods)
        .build();

  }

}
