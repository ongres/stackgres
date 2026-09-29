/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceWriter;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.cluster.context.ClusterPostgresVersionContextAppender;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ShardedClusterReconciliator
    extends AbstractReconciliator<StackGresShardedCluster, StackGresShardedClusterReview> {

  @Dependent
  static class Parameters {
    @Inject OperatorPropertyContext operatorPropertyContext;
    @Inject CustomResourceScanner<StackGresShardedCluster> scanner;
    @Inject CustomResourceFinder<StackGresShardedCluster> finder;
    @Inject MutationPipeline<StackGresShardedCluster, StackGresShardedClusterReview> mutationPipeline;
    @Inject ValidationPipeline<StackGresShardedClusterReview> validationPipeline;
    @Inject CustomResourceWriter<StackGresShardedCluster> writer;
    @Inject AbstractConciliator<StackGresShardedCluster> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresShardedCluster> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject StatusManager<StackGresShardedCluster, Condition> statusManager;
    @Inject EventEmitter<StackGresShardedCluster> eventController;
    @Inject CustomResourceWriter<StackGresShardedCluster> clusterWriter;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
    @Inject Metrics metrics;
  }

  private final StatusManager<StackGresShardedCluster, Condition> statusManager;
  private final EventEmitter<StackGresShardedCluster> eventController;
  private final CustomResourceWriter<StackGresShardedCluster> clusterWriter;
  private final PatchResumer<StackGresShardedCluster> patchResumer;

  @Inject
  public ShardedClusterReconciliator(Parameters parameters) {
    super(
        parameters.operatorPropertyContext,
        parameters.scanner,
        parameters.finder,
        parameters.objectMapper,
        parameters.mutationPipeline,
        parameters.validationPipeline,
        parameters.writer,
        parameters.conciliator,
        parameters.deployedResourcesCache,
        parameters.handlerDelegator,
        parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        parameters.metrics,
        StackGresShardedCluster.KIND);
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.clusterWriter = parameters.clusterWriter;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
  }

  @Override
  protected void setSpecAndStatus(StackGresShardedCluster currentConfig,
      StackGresShardedCluster mutatedAndValidatedConfig) {
    currentConfig.setSpec(mutatedAndValidatedConfig.getSpec());
    currentConfig.setStatus(mutatedAndValidatedConfig.getStatus());
  }

  @Override
  protected StackGresShardedClusterReview createAdmissionReview() {
    return new StackGresShardedClusterReview();
  }

  @Override
  protected Class<StackGresShardedCluster> getResourceClass() {
    return StackGresShardedCluster.class;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresShardedCluster configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected void onPreReconciliation(StackGresShardedCluster config) {
    if (ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()
        .contains(config.getSpec().getPostgres().getVersion())) {
      eventController.sendEvent(ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "SGShardedCluster " + config.getMetadata().getNamespace() + "."
              + config.getMetadata().getName() + " is using PostgreSQL "
              + config.getSpec().getPostgres().getVersion() + ". "
              + ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.get(
                  config.getSpec().getPostgres().getVersion()), config);
    }
  }

  @Override
  protected void onPostReconciliation(StackGresShardedCluster config) {
    statusManager.refreshCondition(config);

    clusterWriter.update(config,
        (currentShardedCluster) -> {
          // status.dbOps is not set by the reconciliation cycle but by the SGShardedDbOps job, that
          // sets it when a major version upgrade starts and removes it when it completes. Take its
          // value from the SGShardedCluster that has just been read so that a change performed while
          // this reconciliation cycle was running is not reverted by the status that was read when
          // the cycle started (see https://gitlab.com/ongresinc/stackgres/-/issues/3241).
          var targetDbOps = Optional.ofNullable(currentShardedCluster.getStatus())
              .map(StackGresShardedClusterStatus::getDbOps)
              .orElse(null);
          if (config.getStatus() != null) {
            config.getStatus().setDbOps(targetDbOps);
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
