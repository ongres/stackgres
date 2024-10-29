/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsEventReason;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
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
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ShardedDbOpsReconciliator
    extends AbstractReconciliator<StackGresShardedDbOps> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresShardedDbOps> scanner;
    @Inject CustomResourceFinder<StackGresShardedDbOps> finder;
    @Inject AbstractConciliator<StackGresShardedDbOps> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresShardedDbOps> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject EventEmitter<StackGresShardedDbOps> eventController;
    @Inject ShardedDbOpsStatusManager statusManager;
    @Inject CustomResourceScheduler<StackGresShardedDbOps> dbOpsScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
  }

  private final EventEmitter<StackGresShardedDbOps> eventController;
  private final PatchResumer<StackGresShardedDbOps> patchResumer;
  private final ShardedDbOpsStatusManager statusManager;
  private final CustomResourceScheduler<StackGresShardedDbOps> dbOpsScheduler;

  @Inject
  public ShardedDbOpsReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        StackGresShardedDbOps.KIND);
    this.eventController = parameters.eventController;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
    this.statusManager = parameters.statusManager;
    this.dbOpsScheduler = parameters.dbOpsScheduler;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresShardedDbOps configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected void onPreReconciliation(StackGresShardedDbOps config) {
  }

  @Override
  protected void onPostReconciliation(StackGresShardedDbOps config) {
    dbOpsScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onConfigCreated(StackGresShardedDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(ShardedDbOpsEventReason.SHARDED_DBOPS_CREATED,
        "ShardedDbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " created: " + resourceChanged, dbOps);
  }

  @Override
  protected void onConfigUpdated(StackGresShardedDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(ShardedDbOpsEventReason.SHARDED_DBOPS_UPDATED,
        "ShardedDbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " updated: " + resourceChanged, dbOps);
  }

  @Override
  protected void onError(Exception ex, StackGresShardedDbOps dbOps) {
    String message = MessageFormatter.arrayFormat(
        "ShardedDbOps reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ShardedDbOpsEventReason.SHARDED_DBOPS_CONFIG_ERROR,
        message + ": " + ex.getMessage(), dbOps);
  }

}
