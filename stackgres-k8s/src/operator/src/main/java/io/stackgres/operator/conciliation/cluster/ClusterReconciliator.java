/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.event.EventEmitterType;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.ClusterPatchResumer;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.validation.cluster.PostgresConfigValidator;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterReconciliator
    extends AbstractReconciliator<StackGresCluster> {

  private static final long VERSION_1_1 = StackGresVersion.V_1_1.getVersionAsNumber();

  public ClusterReconciliator() {
    super(StackGresCluster.KIND);
  }

  private StatusManager<StackGresCluster, StackGresClusterCondition> statusManager;

  private EventEmitter<StackGresCluster> eventController;

  private CustomResourceScheduler<StackGresCluster> clusterScheduler;

  private ClusterPatchResumer patchResumer;

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  public void onPreReconciliation(StackGresCluster config) {
    if (Optional.of(config)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getLabelPrefix)
        .isEmpty()) {
      final long version = StackGresVersion.getStackGresVersionAsNumber(config);
      if (version <= VERSION_1_1) {
        if (config.getStatus() == null) {
          config.setStatus(new StackGresClusterStatus());
        }
        config.getStatus().setLabelPrefix("");
      }
    }

    if (PostgresConfigValidator.BUGGY_PG_VERSIONS.keySet()
        .contains(config.getSpec().getPostgres().getVersion())) {
      eventController.sendEvent(ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "Cluster " + config.getMetadata().getNamespace() + "."
              + config.getMetadata().getName() + " is using PostgreSQL "
              + config.getSpec().getPostgres().getVersion() + ". "
              + PostgresConfigValidator.BUGGY_PG_VERSIONS.get(
                  config.getSpec().getPostgres().getVersion()), config);
    }
  }

  @Override
  public void onPostReconciliation(StackGresCluster config) {
    statusManager.refreshCondition(config);

    clusterScheduler.updateStatus(config,
        StackGresCluster::getStatus, (targetCluster, status) -> {
          var targetPodStatuses = Optional.ofNullable(targetCluster.getStatus())
              .map(StackGresClusterStatus::getPodStatuses)
              .orElse(null);
          var targetDbOps = Optional.ofNullable(targetCluster.getStatus())
              .map(StackGresClusterStatus::getDbOps)
              .orElse(null);
          if (status != null) {
            status.setPodStatuses(targetPodStatuses);
            status.setDbOps(targetDbOps);
          }
          targetCluster.setStatus(status);
        });
  }

  @Override
  public void onConfigCreated(StackGresCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "Cluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " created: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  public void onConfigUpdated(StackGresCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "Cluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " updated: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  public void onError(Exception ex, StackGresCluster cluster) {
    String message = MessageFormatter.arrayFormat(
        "Cluster reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
        message + ": " + ex.getMessage(), cluster);
  }

  @Inject
  public void setStatusManager(
      StatusManager<StackGresCluster, StackGresClusterCondition> statusManager) {
    this.statusManager = statusManager;
  }

  @Inject
  public void setEventController(
      @EventEmitterType(StackGresCluster.class)
          EventEmitter<StackGresCluster> eventController) {
    this.eventController = eventController;
  }

  @Inject
  public void setClusterScheduler(CustomResourceScheduler<StackGresCluster> clusterScheduler) {
    this.clusterScheduler = clusterScheduler;
  }

  @Inject
  public void setResourceComparator(ComparisonDelegator<StackGresCluster> resourceComparator) {
    this.patchResumer = new ClusterPatchResumer(resourceComparator);
  }
}
