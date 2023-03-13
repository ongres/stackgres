/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.validation.cluster.PostgresConfigValidator;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ShardedClusterReconciliator
    extends AbstractReconciliator<StackGresShardedCluster> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresShardedCluster> scanner;
    @Inject Conciliator<StackGresShardedCluster> conciliator;
    @Inject HandlerDelegator<StackGresShardedCluster> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject StatusManager<StackGresShardedCluster, Condition> statusManager;
    @Inject EventEmitter<StackGresShardedCluster> eventController;
    @Inject CustomResourceScheduler<StackGresShardedCluster> clusterScheduler;
    @Inject ComparisonDelegator<StackGresShardedCluster> resourceComparator;
  }

  private final StatusManager<StackGresShardedCluster, Condition> statusManager;
  private final EventEmitter<StackGresShardedCluster> eventController;
  private final CustomResourceScheduler<StackGresShardedCluster> clusterScheduler;
  private final PatchResumer<StackGresShardedCluster> patchResumer;

  @Inject
  public ShardedClusterReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.conciliator, parameters.handlerDelegator,
        parameters.client, StackGresShardedCluster.KIND);
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.clusterScheduler = parameters.clusterScheduler;
    this.patchResumer = new PatchResumer<>(parameters.resourceComparator);
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  public void onPreReconciliation(StackGresShardedCluster config) {
    if (PostgresConfigValidator.BUGGY_PG_VERSIONS.keySet()
        .contains(config.getSpec().getPostgres().getVersion())) {
      eventController.sendEvent(ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "Sharded Cluster " + config.getMetadata().getNamespace() + "."
              + config.getMetadata().getName() + " is using PostgreSQL "
              + config.getSpec().getPostgres().getVersion() + ". "
              + PostgresConfigValidator.BUGGY_PG_VERSIONS.get(
                  config.getSpec().getPostgres().getVersion()), config);
    }
  }

  @Override
  public void onPostReconciliation(StackGresShardedCluster config) {
    statusManager.refreshCondition(config);

    clusterScheduler.update(config,
        (targetShardedCluster, shardedClusterWithStatus) -> {
          if (shardedClusterWithStatus.getStatus() != null) {
            targetShardedCluster.setStatus(shardedClusterWithStatus.getStatus());
          }
        });
  }

  @Override
  public void onConfigCreated(StackGresShardedCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "Sharded Cluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " created: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  public void onConfigUpdated(StackGresShardedCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "Sharded Cluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " updated: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  public void onError(Exception ex, StackGresShardedCluster cluster) {
    String message = MessageFormatter.arrayFormat(
        "Cluster reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
        message + ": " + ex.getMessage(), cluster);
  }

}
