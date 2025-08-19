/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.ClusterPatchResumer;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.cluster.context.ClusterPostgresVersionContextAppender;
import io.stackgres.operator.conciliation.factory.dbops.DbOpsClusterRollout;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterReconciliator
    extends AbstractReconciliator<StackGresCluster> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresCluster> scanner;
    @Inject CustomResourceFinder<StackGresCluster> finder;
    @Inject AbstractConciliator<StackGresCluster> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresCluster> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject StatusManager<StackGresCluster, Condition> statusManager;
    @Inject EventEmitter<StackGresCluster> eventController;
    @Inject CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
    @Inject Metrics metrics;
  }

  private final StatusManager<StackGresCluster, Condition> statusManager;
  private final EventEmitter<StackGresCluster> eventController;
  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final ClusterPatchResumer patchResumer;

  @Inject
  public ClusterReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        parameters.metrics,
        StackGresCluster.KIND);
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.clusterScheduler = parameters.clusterScheduler;
    this.patchResumer = new ClusterPatchResumer(parameters.objectMapper);
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresCluster configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected void onPreReconciliation(StackGresCluster config) {
    if (Optional.of(config)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPostgresVersion)
        .map(ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains)
        .orElse(false)) {
      eventController.sendEvent(ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "Cluster " + config.getMetadata().getNamespace() + "."
              + config.getMetadata().getName() + " is using PostgreSQL "
              + config.getSpec().getPostgres().getVersion() + ". "
              + ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.get(
                  config.getSpec().getPostgres().getVersion()), config);
    }
  }

  @Override
  protected void onPostReconciliation(StackGresCluster config) {
    statusManager.refreshCondition(config);

    clusterScheduler.update(config,
        (currentCluster) -> {
          currentCluster.getMetadata().setAnnotations(
              Seq.seq(
                  Optional.ofNullable(currentCluster.getMetadata().getAnnotations())
                  .map(Map::entrySet)
                  .stream()
                  .flatMap(Set::stream)
                  .filter(annotation -> !Objects.equals(annotation.getKey(), StackGresContext.VERSION_KEY))
                  .filter(annotation -> !DbOpsClusterRollout.ROLLOUT_DBOPS_KEYS.contains(annotation.getKey())
                      || Optional.ofNullable(config.getStatus())
                      .map(StackGresClusterStatus::getDbOps)
                      .map(StackGresClusterDbOpsStatus::getName)
                      .isPresent()))
              .append(Optional.ofNullable(config.getMetadata().getAnnotations())
                  .map(Map::entrySet)
                  .stream()
                  .flatMap(Set::stream)
                  .filter(annotation -> Objects.equals(annotation.getKey(), StackGresContext.VERSION_KEY)))
              .toMap(Map.Entry::getKey, Map.Entry::getValue));
          var targetOs = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getOs)
              .orElse(null);
          var targetArch = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getArch)
              .orElse(null);
          var targetPodStatuses = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getPodStatuses)
              .orElse(null);
          var targetDbOps = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getDbOps)
              .orElse(null);
          var targetManagedSql = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getManagedSql)
              .orElse(null);
          if (config.getStatus() != null) {
            config.getStatus().setOs(targetOs);
            config.getStatus().setArch(targetArch);
            config.getStatus().setPodStatuses(targetPodStatuses);
            config.getStatus().setDbOps(targetDbOps);
            config.getStatus().setManagedSql(targetManagedSql);
            currentCluster.setStatus(config.getStatus());
          }
        });
  }

  @Override
  protected void onConfigCreated(StackGresCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "SGCluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " created: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  protected void onConfigUpdated(StackGresCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "SGCluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " updated: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  protected void onError(Exception ex, StackGresCluster cluster) {
    String message = MessageFormatter.arrayFormat(
        "SGCluster reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
        message + ": " + ex.getMessage(), cluster);
  }

}
