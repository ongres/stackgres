/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterControllerReconciliator
    extends Reconciliator<StackGresClusterContext> {

  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final ClusterExtensionReconciliator extensionReconciliator;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final ClusterControllerPropertyContext propertyContext;

  @Inject
  public ClusterControllerReconciliator(Parameters parameters) {
    this.clusterScheduler = parameters.clusterScheduler;
    this.extensionReconciliator = parameters.extensionReconciliator;
    this.clusterFinder = parameters.clusterFinder;
    this.propertyContext = parameters.propertyContext;
  }

  public ClusterControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.propertyContext = null;
    this.clusterScheduler = null;
    this.extensionReconciliator = null;
    this.clusterFinder = null;
  }

  private static void applyPodStatusChanges(StackGresClusterPodStatus podStatus,
                                            StackGresClusterPodStatus savedPodStatus) {
    savedPodStatus.setInstalledPostgresExtensions(
        podStatus.getInstalledPostgresExtensions());
    savedPodStatus.setPendingRestart(podStatus.getPendingRestart());
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  @Override
  protected ReconciliationResult<?> reconcile(KubernetesClient client,
                                              StackGresClusterContext context) throws Exception {
    ReconciliationResult<Boolean> extensionReconciliationResult =
        extensionReconciliator.reconcile(client, context);

    if (extensionReconciliationResult.result().orElse(false)) {

      final String podName = propertyContext.getString(
          ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
      final StackGresCluster cluster = context.getCluster();
      final StackGresClusterStatus status = cluster.getStatus();

      String clusterName = cluster.getMetadata().getName();
      String namespace = cluster.getMetadata().getNamespace();

      clusterFinder.findByNameAndNamespace(clusterName, namespace)
          .ifPresent(savedCluster -> {
            var newPodStatus = findPodStatus(status.getPodStatuses(), podName)
                .orElseThrow();
            Optional.ofNullable(savedCluster.getStatus()).ifPresentOrElse(
                savedStatus ->
                    Optional.ofNullable(savedStatus.getPodStatuses()).ifPresentOrElse(
                        savedPodStatuses ->
                            findPodStatus(savedPodStatuses, podName).ifPresentOrElse(
                                savedPodStatus ->
                                    applyPodStatusChanges(newPodStatus, savedPodStatus),
                                () -> savedPodStatuses.add(newPodStatus)),
                        () -> savedStatus.setPodStatuses(status.getPodStatuses())),
                () -> savedCluster.setStatus(status));

            clusterScheduler.updateStatus(savedCluster);
          });

    }
    return extensionReconciliationResult;
  }

  private Optional<StackGresClusterPodStatus> findPodStatus(
      List<StackGresClusterPodStatus> podStatuses,
      String podName) {
    return podStatuses.stream().filter(podStatus -> podStatus.getName().equals(podName))
        .findFirst();
  }

  @Dependent
  public static class Parameters {
    @Inject
    CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject
    ClusterExtensionReconciliator extensionReconciliator;
    @Inject
    CustomResourceFinder<StackGresCluster> clusterFinder;
    @Inject
    ClusterControllerPropertyContext propertyContext;
  }

}
