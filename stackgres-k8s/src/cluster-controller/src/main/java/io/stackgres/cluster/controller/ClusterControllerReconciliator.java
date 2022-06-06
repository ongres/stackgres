/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.ArrayList;
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
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;
import org.jooq.lambda.tuple.Tuple;

@ApplicationScoped
public class ClusterControllerReconciliator
    extends Reconciliator<StackGresClusterContext> {

  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final ClusterControllerPostgresBootstrapReconciliator postgresBootstrapReconciliator;
  private final ClusterExtensionReconciliator extensionReconciliator;
  private final PgBouncerReconciliator pgbouncerReconciliator;
  private final ClusterPersistentVolumeSizeReconciliator pvcSizeReconciliator;
  private final PatroniReconciliator patroniReconciliator;
  private final String podName;

  @Inject
  public ClusterControllerReconciliator(Parameters parameters) {
    this.clusterScheduler = parameters.clusterScheduler;
    this.postgresBootstrapReconciliator = parameters.postgresBootstrapReconciliator;
    this.extensionReconciliator = parameters.extensionReconciliator;
    this.pgbouncerReconciliator = parameters.pgbouncerReconciliator;
    this.pvcSizeReconciliator = parameters.clusterPersistentVolumeSizeReconciliator;
    this.patroniReconciliator = parameters.patroniReconciliator;
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
  }

  public ClusterControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.clusterScheduler = null;
    this.postgresBootstrapReconciliator = null;
    this.extensionReconciliator = null;
    this.pgbouncerReconciliator = null;
    this.pvcSizeReconciliator = null;
    this.patroniReconciliator = null;
    this.podName = null;
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  @Override
  protected ReconciliationResult<?> reconcile(KubernetesClient client,
      StackGresClusterContext context) throws Exception {
    final StackGresCluster cluster = context.getCluster();
    final boolean podStatusMissing = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .map(StackGresClusterPodStatus::getName)
        .noneMatch(podName::equals);
    if (podStatusMissing) {
      if (cluster.getStatus() == null) {
        cluster.setStatus(new StackGresClusterStatus());
      }
      if (cluster.getStatus().getPodStatuses() == null) {
        cluster.getStatus().setPodStatuses(new ArrayList<>());
      }
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName(podName);
      podStatus.setPendingRestart(false);
      cluster.getStatus().getPodStatuses().add(podStatus);
    }

    ReconciliationResult<Void> postgresBootstrapReconciliatorResult =
        postgresBootstrapReconciliator.reconcile(client, context);
    ReconciliationResult<Boolean> extensionReconciliationResult =
        extensionReconciliator.reconcile(client, context);
    ReconciliationResult<Void> pgbouncerReconciliationResult =
        pgbouncerReconciliator.reconcile(client, context);
    ReconciliationResult<Boolean> patroniReconciliationResult =
        patroniReconciliator.reconcile(client, context);

    if (podStatusMissing
        || extensionReconciliationResult.result().orElse(false)
        || patroniReconciliationResult.result().orElse(false)) {
      clusterScheduler.update(cluster,
          (targetCluster, clusterWithStatus) -> {
            var podStatus = Optional.ofNullable(clusterWithStatus.getStatus())
                .map(StackGresClusterStatus::getPodStatuses)
                .flatMap(podStatuses -> findPodStatus(podStatuses, podName))
                .orElseThrow();
            if (targetCluster.getStatus() == null) {
              targetCluster.setStatus(new StackGresClusterStatus());
            }
            targetCluster.getStatus().setArch(clusterWithStatus.getStatus().getArch());
            targetCluster.getStatus().setOs(clusterWithStatus.getStatus().getOs());
            if (targetCluster.getStatus().getPodStatuses() == null) {
              targetCluster.getStatus().setPodStatuses(new ArrayList<>());
            }
            findPodStatus(targetCluster.getStatus().getPodStatuses(), podName)
                .ifPresentOrElse(
                    targetPodStatus -> {
                      targetCluster.getStatus().getPodStatuses().set(
                          targetCluster.getStatus().getPodStatuses().indexOf(targetPodStatus),
                          podStatus);
                    },
                    () -> targetCluster.getStatus().getPodStatuses().add(podStatus));
          });
    }

    if (extensionReconciliationResult.result().orElse(false)) {
      KubernetesClientUtil.retryOnConflict(() -> clusterScheduler.update(cluster,
          (targetCluster, sourceCluster) -> {
            sourceCluster.getSpec().getToInstallPostgresExtensions().stream()
                .filter(toInstallExtension -> targetCluster.getSpec()
                    .getToInstallPostgresExtensions()
                    .stream().noneMatch(toInstallExtension::equals))
                .map(toInstallExtension -> Tuple.tuple(toInstallExtension,
                    targetCluster.getSpec().getToInstallPostgresExtensions().stream()
                    .filter(targetToInstallExtension -> toInstallExtension.getName()
                        .equals(targetToInstallExtension.getName()))
                    .findFirst()))
                .filter(t -> t.v2.isPresent())
                .map(t -> t.map2(Optional::get))
                .forEach(t -> t.v1.setBuild(t.v2.getBuild()));
          }));
    }

    pvcSizeReconciliator.reconcile();

    return postgresBootstrapReconciliatorResult
        .join(extensionReconciliationResult)
        .join(pgbouncerReconciliationResult)
        .join(patroniReconciliationResult);
  }

  private Optional<StackGresClusterPodStatus> findPodStatus(
      List<StackGresClusterPodStatus> podStatuses,
      String podName) {
    return podStatuses.stream()
        .filter(podStatus -> podStatus.getName().equals(podName))
        .findFirst();
  }

  @Dependent
  public static class Parameters {
    @Inject CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject ClusterControllerPostgresBootstrapReconciliator postgresBootstrapReconciliator;
    @Inject ClusterExtensionReconciliator extensionReconciliator;
    @Inject PgBouncerReconciliator pgbouncerReconciliator;
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject ClusterPersistentVolumeSizeReconciliator clusterPersistentVolumeSizeReconciliator;
    @Inject PatroniReconciliator patroniReconciliator;
  }

}
