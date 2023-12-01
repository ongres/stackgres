/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusDatabase;
import io.stackgres.common.distributedlogs.Tables;
import io.stackgres.distributedlogs.common.DistributedLogsControllerEventReason;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.distributedlogs.configuration.DistributedLogsControllerPropertyContext;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsClusterReconciliator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      DistributedLogsClusterReconciliator.class);

  private final DistributedLogsControllerPropertyContext propertyContext;
  private final DistributedLogsDatabaseManager databaseManager;
  private final DistributedLogsConfigManager configManager;
  private final EventController eventController;
  private final DistributedLogsPersistentVolumeSizeReconciliator pvcSizeReconciliator;

  @Dependent
  public static class Parameters {
    @Inject DistributedLogsControllerPropertyContext propertyContext;
    @Inject DistributedLogsDatabaseManager databaseManager;
    @Inject DistributedLogsConfigManager configReconciliator;
    @Inject EventController eventController;
    @Inject DistributedLogsPersistentVolumeSizeReconciliator persistentVolumeSizeReconciliator;
  }

  @Inject
  public DistributedLogsClusterReconciliator(Parameters parameters) {
    this.propertyContext = parameters.propertyContext;
    this.databaseManager = parameters.databaseManager;
    this.configManager = parameters.configReconciliator;
    this.eventController = parameters.eventController;
    this.pvcSizeReconciliator = parameters.persistentVolumeSizeReconciliator;
  }

  public static DistributedLogsClusterReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsClusterReconciliator(parameters.findAny().get());
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  protected ReconciliationResult<Boolean> reconcile(
      KubernetesClient client, StackGresDistributedLogsContext context) throws Exception {
    pvcSizeReconciliator.reconcile();
    StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    if (distributedLogs.getStatus() == null || !isPatroniReady(context)) {
      LOGGER.warn("Waiting for distributedlogs cluster to become ready...");
      return new ReconciliationResult<>(false);
    }
    final ImmutableList.Builder<Exception> exceptions = ImmutableList.builder();
    boolean statusUpdated = false;
    for (StackGresDistributedLogsStatusCluster cluster : distributedLogs
        .getStatus().getConnectedClusters()) {
      String database = FluentdUtil.databaseName(cluster.getNamespace(), cluster.getName());
      try {
        if (!databaseManager.existsDatabase(context, database)) {
          LOGGER.info("Creating database {}", database);
          databaseManager.createDatabase(context, database);
        }
      } catch (Exception ex) {
        exceptions.add(ex);
        handleException(client, distributedLogs, cluster, ex);
        continue;
      }
      String retention = cluster.getConfig().getRetention();
      if (!Optional.of(distributedLogs.getStatus().getDatabases())
          .flatMap(databases -> databases.stream()
              .filter(databaseStatus -> databaseStatus.getName().equals(database))
              .findAny())
          .map(StackGresDistributedLogsStatusDatabase::getRetention)
          .map(currentRetention -> Objects.equals(retention, currentRetention))
          .orElse(false)) {
        for (String table : Seq.of(Tables.values()).map(Tables::getTableName)) {
          LOGGER.info("Updating retention window for database {} and table to {}", database,
              retention);
          try {
            databaseManager.updateRetention(context, database, retention, table);
          } catch (Exception ex) {
            exceptions.add(ex);
            handleException(client, distributedLogs, cluster, ex);
            continue;
          }
        }
      }
      if (retention != null) {
        for (String table : Seq.of(Tables.values()).map(Tables::getTableName)) {
          try {
            databaseManager.reconcileRetention(context, database, retention, table)
                .stream()
                .forEach(output -> LOGGER.info(
                    "Reconcile retention for database {} and table {}: {}",
                    database, table, output));
          } catch (Exception ex) {
            exceptions.add(ex);
            handleException(client, distributedLogs, cluster, ex);
            continue;
          }
        }
      }
      statusUpdated = statusUpdated || updateStatus(distributedLogs, database, retention);
    }

    String fluentdConfigHash = configManager.getFluentdConfigHash();
    distributedLogs.getStatus().setFluentdConfigHash(fluentdConfigHash);
    configManager.reloadFluentdConfiguration();
    return new ReconciliationResult<>(statusUpdated, exceptions.build());
  }

  private boolean isPatroniReady(StackGresDistributedLogsContext context) {
    return context.getExistingResources().stream()
    .map(Tuple2::v1)
    .filter(Pod.class::isInstance)
    .map(Pod.class::cast)
    .anyMatch(pod -> pod.getMetadata().getName().equals(
        propertyContext.getString(
            DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME))
        && Optional.ofNullable(pod.getStatus())
        .map(PodStatus::getContainerStatuses)
        .filter(containerStatuses -> containerStatuses.stream()
            .anyMatch(containerStatus -> containerStatus.getName().equals(
                StackGresContainer.PATRONI.getName())
                && containerStatus.getReady()))
        .map(containersWithReadyPatroni -> true)
        .orElse(false));
  }

  private boolean updateStatus(StackGresDistributedLogs distributedLogs, String database,
      String retention) {
    Optional<StackGresDistributedLogsStatusDatabase> foundDistributedLogsDatabase =
        distributedLogs.getStatus().getDatabases()
        .stream()
        .filter(databaseStatus -> databaseStatus.getName().equals(database))
        .findAny();
    final StackGresDistributedLogsStatusDatabase distributedLogsDatabase =
        foundDistributedLogsDatabase.orElseGet(() -> new StackGresDistributedLogsStatusDatabase());
    if (foundDistributedLogsDatabase.isEmpty()) {
      distributedLogs.getStatus().getDatabases().add(distributedLogsDatabase);
    }

    if (Objects.isNull(distributedLogsDatabase.getName())
        || !Objects.equals(retention, distributedLogsDatabase.getRetention())) {
      distributedLogsDatabase.setName(database);
      distributedLogsDatabase.setRetention(retention);
      return true;
    }

    return false;
  }

  private void handleException(KubernetesClient client, StackGresDistributedLogs distributedLogs,
      StackGresDistributedLogsStatusCluster cluster, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres DistributeLogs {}.{} reconciliation failed for cluster {}.{}",
        new String[] {
            distributedLogs.getMetadata().getNamespace(),
            distributedLogs.getMetadata().getName(),
            cluster.getNamespace(),
            cluster.getName(),
        }).getMessage();
    LOGGER.error(message, ex);
    try {
      eventController.sendEvent(
          DistributedLogsControllerEventReason.DISTRIBUTEDLOGS_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), distributedLogs, client);
    } catch (RuntimeException rex) {
      LOGGER.error("Failed sending event while reconciling cluster {}.{}",
          cluster.getNamespace(), cluster.getName(), rex);
    }
  }

}
