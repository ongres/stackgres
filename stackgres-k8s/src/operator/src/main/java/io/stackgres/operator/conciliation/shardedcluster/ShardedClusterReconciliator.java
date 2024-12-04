/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.validation.cluster.PostgresConfigValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ShardedClusterReconciliator
    extends AbstractReconciliator<StackGresShardedCluster> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresShardedCluster> scanner;
    @Inject CustomResourceFinder<StackGresShardedCluster> finder;
    @Inject AbstractConciliator<StackGresShardedCluster> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresShardedCluster> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject StatusManager<StackGresShardedCluster, Condition> statusManager;
    @Inject EventEmitter<StackGresShardedCluster> eventController;
    @Inject CustomResourceScheduler<StackGresShardedCluster> clusterScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
  }

  private final StatusManager<StackGresShardedCluster, Condition> statusManager;
  private final EventEmitter<StackGresShardedCluster> eventController;
  private final CustomResourceScheduler<StackGresShardedCluster> clusterScheduler;
  private final PatchResumer<StackGresShardedCluster> patchResumer;

  @Inject
  public ShardedClusterReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        StackGresShardedCluster.KIND);
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.clusterScheduler = parameters.clusterScheduler;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresShardedCluster configKey, boolean load) {
    super.reconciliationCycle(configKey, load);
  }

  @Override
  protected void onPreReconciliation(StackGresShardedCluster config) {
    if (PostgresConfigValidator.BUGGY_PG_VERSIONS.keySet()
        .contains(config.getSpec().getPostgres().getVersion())) {
      eventController.sendEvent(ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "SGShardedCluster " + config.getMetadata().getNamespace() + "."
              + config.getMetadata().getName() + " is using PostgreSQL "
              + config.getSpec().getPostgres().getVersion() + ". "
              + PostgresConfigValidator.BUGGY_PG_VERSIONS.get(
                  config.getSpec().getPostgres().getVersion()), config);
    }
  }

  @Override
  protected void onPostReconciliation(StackGresShardedCluster config) {
    statusManager.refreshCondition(config);

    clusterScheduler.update(config,
        (currentShardedCluster) -> {
          if (config.getStatus() != null) {
            currentShardedCluster.setStatus(config.getStatus());
          }
        });
  }

  @Override
  protected void onConfigCreated(StackGresShardedCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "SGShardedCluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " created: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  protected void onConfigUpdated(StackGresShardedCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "SGShardedCluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " updated: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  protected void onError(Exception ex, StackGresShardedCluster cluster) {
    String message = MessageFormatter.arrayFormat(
        "SGShardedCluster reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
        message + ": " + ex.getMessage(), cluster);
  }

}
