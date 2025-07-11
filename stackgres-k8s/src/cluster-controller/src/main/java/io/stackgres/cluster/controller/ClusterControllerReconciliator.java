/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
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
  private final ManagedSqlReconciliator managedSqlReconciliator;
  private final SslReconciliator sslReconciliator;
  private final PatroniStandbyReconciliator patroniStandbyReconciliator;
  private final PatroniConfigReconciliator patroniConfigReconciliator;
  private final PatroniMajorVersionUpgradeReconciliator patroniMajorVersionUpgradeReconciliator;
  private final PatroniBackupFailoverRestartReconciliator patroniBackupFailoverRestartReconciliator;
  private final ClusterControllerPropertyContext propertyContext;
  private final String podName;
  private final Optional<String> nodeName;

  @Inject
  public ClusterControllerReconciliator(Parameters parameters) {
    this.clusterScheduler = parameters.clusterScheduler;
    this.postgresBootstrapReconciliator = parameters.postgresBootstrapReconciliator;
    this.extensionReconciliator = parameters.extensionReconciliator;
    this.pgbouncerReconciliator = parameters.pgbouncerReconciliator;
    this.pvcSizeReconciliator = parameters.clusterPersistentVolumeSizeReconciliator;
    this.patroniReconciliator = parameters.patroniReconciliator;
    this.managedSqlReconciliator = parameters.managedSqlReconciliator;
    this.sslReconciliator = parameters.sslReconciliator;
    this.patroniStandbyReconciliator = parameters.patroniStandbyReconciliator;
    this.patroniConfigReconciliator = parameters.patroniConfigReconciliator;
    this.patroniMajorVersionUpgradeReconciliator = parameters.patroniMajorVersionUpgradeReconciliator;
    this.patroniBackupFailoverRestartReconciliator = parameters.patroniBackupFailoverRestartReconciliator;
    this.propertyContext = parameters.propertyContext;
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.nodeName = parameters.propertyContext
        .get(ClusterControllerProperty.CLUSTER_CONTROLLER_NODE_NAME);
  }

  public ClusterControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.clusterScheduler = null;
    this.postgresBootstrapReconciliator = null;
    this.extensionReconciliator = null;
    this.pgbouncerReconciliator = null;
    this.pvcSizeReconciliator = null;
    this.patroniReconciliator = null;
    this.managedSqlReconciliator = null;
    this.sslReconciliator = null;
    this.patroniStandbyReconciliator = null;
    this.patroniConfigReconciliator = null;
    this.patroniMajorVersionUpgradeReconciliator = null;
    this.patroniBackupFailoverRestartReconciliator = null;
    this.propertyContext = null;
    this.podName = null;
    this.nodeName = null;
  }

  @Override
  public ReconciliationResult<Void> reconcile(KubernetesClient client,
      StackGresClusterContext context) throws Exception {
    final StackGresCluster cluster = context.getCluster();
    final Optional<StackGresClusterPodStatus> foundPodStatus =
        Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .filter(podStatus -> Objects.equals(podName, podStatus.getName()))
        .findFirst();
    final boolean nodeNameChanged;
    if (foundPodStatus.isPresent()) {
      String nodeName = this.nodeName.orElseGet(foundPodStatus.get()::getNodeName);
      nodeNameChanged = Objects.equals(foundPodStatus.get().getNodeName(), nodeName);
      foundPodStatus.get().setNodeName(nodeName);
    } else {
      nodeNameChanged = true;
      if (cluster.getStatus() == null) {
        cluster.setStatus(new StackGresClusterStatus());
      }
      if (cluster.getStatus().getPodStatuses() == null) {
        cluster.getStatus().setPodStatuses(new ArrayList<>());
      }
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName(podName);
      nodeName.ifPresent(podStatus::setNodeName);
      podStatus.setPrimary(false);
      podStatus.setPendingRestart(false);
      cluster.getStatus().getPodStatuses().add(podStatus);
    }

    ReconciliationResult<Boolean> postgresBootstrapReconciliatorResult =
        postgresBootstrapReconciliator.reconcile(client, context);
    ReconciliationResult<Boolean> extensionReconciliationResult =
        extensionReconciliator.reconcile(client, context);
    ReconciliationResult<Void> pgbouncerReconciliationResult =
        pgbouncerReconciliator.reconcile(client, context);
    ReconciliationResult<Boolean> patroniReconciliationResult =
        patroniReconciliator.reconcile(client, context);
    ReconciliationResult<Boolean> managedSqlReconciliationResult =
        managedSqlReconciliator.reconcile(client, context);
    ReconciliationResult<Void> postgresSslReconciliationResult =
        sslReconciliator.reconcile(client, context);
    ReconciliationResult<Void> patroniStandbyReconciliatorResult =
        patroniStandbyReconciliator.reconcile(client, context);
    ReconciliationResult<Void> patroniConfigReconciliationResult =
        patroniConfigReconciliator.reconcile(client, context);
    ReconciliationResult<Void> patroniMajorVersionUpgradeReconciliatorResult =
        patroniMajorVersionUpgradeReconciliator.reconcile(client, context);
    ReconciliationResult<Void> patroniBackupFailoverRestartReconciliatorResult =
        patroniBackupFailoverRestartReconciliator.reconcile(client, context);

    if (foundPodStatus.isEmpty()
        || nodeNameChanged
        || postgresBootstrapReconciliatorResult.result().orElse(false)
        || extensionReconciliationResult.result().orElse(false)
        || patroniReconciliationResult.result().orElse(false)) {
      clusterScheduler.update(cluster,
          (currentCluster) -> updateClusterPodStatus(currentCluster, cluster));
    }

    if (extensionReconciliationResult.result().orElse(false)) {
      KubernetesClientUtil.retryOnConflict(() -> clusterScheduler.update(cluster,
          (currentCluster) -> {
            cluster.getSpec().getToInstallPostgresExtensions().stream()
                .filter(toInstallExtension -> currentCluster.getSpec()
                    .getToInstallPostgresExtensions()
                    .stream().noneMatch(toInstallExtension::equals))
                .map(toInstallExtension -> Tuple.tuple(toInstallExtension,
                    currentCluster.getSpec().getToInstallPostgresExtensions().stream()
                    .filter(targetToInstallExtension -> toInstallExtension.getName()
                        .equals(targetToInstallExtension.getName()))
                    .findFirst()))
                .filter(t -> t.v2.isPresent())
                .map(t -> t.map2(Optional::get))
                .forEach(t -> t.v1.setBuild(t.v2.getBuild()));
          }));
    }

    var pvcSizeReconciliatorResult = pvcSizeReconciliator.reconcile(client, propertyContext);

    return postgresBootstrapReconciliatorResult
        .join(extensionReconciliationResult)
        .join(pgbouncerReconciliationResult)
        .join(patroniReconciliationResult)
        .join(managedSqlReconciliationResult)
        .join(postgresSslReconciliationResult)
        .join(patroniStandbyReconciliatorResult)
        .join(patroniConfigReconciliationResult)
        .join(patroniMajorVersionUpgradeReconciliatorResult)
        .join(patroniBackupFailoverRestartReconciliatorResult)
        .join(pvcSizeReconciliatorResult);
  }

  private void updateClusterPodStatus(StackGresCluster currentCluster,
      StackGresCluster cluster) {
    var podStatus = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .flatMap(podStatuses -> findPodStatus(podStatuses, podName))
        .orElseThrow();
    if (currentCluster.getStatus() == null) {
      currentCluster.setStatus(new StackGresClusterStatus());
    }
    currentCluster.getStatus().setArch(cluster.getStatus().getArch());
    currentCluster.getStatus().setOs(cluster.getStatus().getOs());
    if (currentCluster.getStatus().getPodStatuses() == null) {
      currentCluster.getStatus().setPodStatuses(new ArrayList<>());
    }
    findPodStatus(currentCluster.getStatus().getPodStatuses(), podName)
        .ifPresentOrElse(
            targetPodStatus -> {
              currentCluster.getStatus().getPodStatuses().set(
                  currentCluster.getStatus().getPodStatuses().indexOf(targetPodStatus),
                  podStatus);
            },
            () -> currentCluster.getStatus().getPodStatuses().add(podStatus));
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
    @Inject ManagedSqlReconciliator managedSqlReconciliator;
    @Inject SslReconciliator sslReconciliator;
    @Inject PatroniStandbyReconciliator patroniStandbyReconciliator;
    @Inject PatroniConfigReconciliator patroniConfigReconciliator;
    @Inject PatroniMajorVersionUpgradeReconciliator patroniMajorVersionUpgradeReconciliator;
    @Inject PatroniBackupFailoverRestartReconciliator patroniBackupFailoverRestartReconciliator;
  }

}
