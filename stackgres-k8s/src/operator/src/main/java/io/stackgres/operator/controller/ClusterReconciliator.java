/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ObjectMapperProvider;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.BackupEventReason;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.ClusterResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.ResourceGeneratorReconciliator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterReconciliator
    extends ResourceGeneratorReconciliator<StackGresClusterContext, StackGresCluster,
      ClusterResourceHandlerSelector> {

  private final ClusterStatusManager statusManager;
  private final EventController eventController;
  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final CustomResourceScheduler<StackGresBackup> backupScheduler;

  @Dependent
  public static class Parameters {
    @Inject ClusterResourceHandlerSelector handlerSelector;
    @Inject ObjectMapperProvider objectMapperProvider;
    @Inject ClusterStatusManager statusManager;
    @Inject EventController eventController;
    @Inject CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject CustomResourceScheduler<StackGresBackup> backupScheduler;
  }

  @Inject
  public ClusterReconciliator(Parameters parameters) {
    super("Cluster", StackGresClusterContext::getCluster,
        parameters.handlerSelector, parameters.objectMapperProvider.objectMapper());
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.clusterScheduler = parameters.clusterScheduler;
    this.backupScheduler = parameters.backupScheduler;
  }

  public ClusterReconciliator() {
    super(null, c -> null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.statusManager = null;
    this.eventController = null;
    this.clusterScheduler = null;
    this.backupScheduler = null;
  }

  public static ClusterReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new ClusterReconciliator(parameters.findAny().get());
  }

  @Override
  protected void onPreConfigReconcilied(KubernetesClient client, StackGresClusterContext context) {
    statusManager.updatePendingRestart(context);
    boolean isRestartPending = statusManager.isPendingRestart(context);
    context.getRequiredResources().stream()
        .map(Tuple2::v1)
        .forEach(resource -> {
          if (!isRestartPending
              && Optional.ofNullable(resource.getMetadata())
              .map(ObjectMeta::getAnnotations)
              .map(annotations -> annotations
                  .get(StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY))
              .map(Boolean::valueOf)
              .orElse(false)) {
            Map<String, String> annotations = new HashMap<>(
                resource.getMetadata().getAnnotations());
            annotations.put(
                StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY,
                String.valueOf(Boolean.FALSE));
            resource.getMetadata().setAnnotations(annotations);
          }
        });
    Seq.seq(context.getBackups())
        .filter(backup -> Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getStatus)
            .map(status -> false)
            .orElse(true))
        .forEach(backup -> {
          try {
            backup.setStatus(new StackGresBackupStatus());
            backup.getStatus().setProcess(new StackGresBackupProcess());
            backup.getStatus().getProcess().setStatus(BackupPhase.PENDING.label());
            backupScheduler.update(backup);
          } catch (Exception ex) {
            logger.error("Error while setting backup status to " + BackupPhase.PENDING.label(), ex);
          }
        });

    final StackGresCluster cluster = context.getCluster();
    Seq.seq(context.getBackups())
        .forEach(backup -> {
          if (Optional.ofNullable(backup.getStatus())
              .map(StackGresBackupStatus::getProcess)
              .map(StackGresBackupProcess::getStatus)
              .map(BackupPhase.PENDING.label()::equals)
              .orElse(false)) {
            if (!context.getBackupContext().isPresent()) {
              eventController.sendEvent(BackupEventReason.BACKUP_CONFIG_ERROR,
                  "Missing " + StackGresBackupConfig.KIND + " for cluster "
                  + cluster.getMetadata().getNamespace() + "."
                  + cluster.getMetadata().getName() + " ", backup, client);
            }
          }
        });
  }

  @Override
  protected void onConfigCreated(KubernetesClient client, StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
        + cluster.getMetadata().getName() + " created", cluster, client);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), context);
    clusterScheduler.updateStatus(context.getCluster(),
        StackGresCluster::getStatus,
        StackGresCluster::setStatus);
  }

  @Override
  protected void onConfigUpdated(KubernetesClient client, StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
        + cluster.getMetadata().getName() + " updated", cluster, client);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), context);
    clusterScheduler.updateStatus(context.getCluster(),
        StackGresCluster::getStatus,
        StackGresCluster::setStatus);
  }

  @Override
  protected void onPostConfigReconcilied(KubernetesClient client, StackGresClusterContext context) {
    statusManager.updatePendingRestart(context);
    clusterScheduler.updateStatus(context.getCluster(),
        StackGresCluster::getStatus,
        StackGresCluster::setStatus);
  }

}
