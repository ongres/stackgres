/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusDatabase;
import io.stackgres.common.distributedlogs.Tables;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.distributedlogs.common.DistributedLogsEventReason;
import io.stackgres.distributedlogs.common.DistributedLogsProperty;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.distributedlogs.configuration.DistributedLogsPropertyContext;
import io.stackgres.operatorframework.reconciliation.Reconciliator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsReconciliator
    extends Reconciliator<StackGresDistributedLogsContext> {

  private final DistributedLogsPropertyContext propertyContext;
  private final DistributedLogsDatabaseReconciliator databaseReconciliator;
  private final DistributedLogsConfigReconciliator configReconciliator;
  private final CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject DistributedLogsPropertyContext propertyContext;
    @Inject DistributedLogsDatabaseReconciliator databaseReconciliator;
    @Inject DistributedLogsConfigReconciliator configReconciliator;
    @Inject CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
    @Inject EventController eventController;
  }

  @Inject
  public DistributedLogsReconciliator(Parameters parameters) {
    this.propertyContext = parameters.propertyContext;
    this.databaseReconciliator = parameters.databaseReconciliator;
    this.configReconciliator = parameters.configReconciliator;
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
    this.eventController = parameters.eventController;
  }

  public DistributedLogsReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.propertyContext = null;
    this.databaseReconciliator = null;
    this.configReconciliator = null;
    this.distributedLogsScheduler = null;
    this.eventController = null;
  }

  public static DistributedLogsReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsReconciliator(parameters.findAny().get());
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  @Override
  protected void reconcile(KubernetesClient client, StackGresDistributedLogsContext context)
      throws Exception {
    StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    if (distributedLogs.getStatus() == null || !isPatroniReady(context)) {
      logger.warn("Waiting for distributedlogs cluster to become ready...");
      TimeUnit.SECONDS.sleep(1);
      return;
    }
    for (StackGresDistributedLogsStatusCluster cluster : distributedLogs
        .getStatus().getConnectedClusters()) {
      String database = FluentdUtil.databaseName(cluster.getNamespace(), cluster.getName());
      try {
        if (!databaseReconciliator.existsDatabase(context, database)) {
          logger.info("Creating database {}", database);
          databaseReconciliator.createDatabase(context, database);
        }
      } catch (Exception ex) {
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
          logger.info("Updating retention window for database {} and table to {}", database,
              retention);
          try {
            databaseReconciliator.updateRetention(context, database, retention, table);
          } catch (Exception ex) {
            handleException(client, distributedLogs, cluster, ex);
            continue;
          }
        }
      }
      if (retention != null) {
        for (String table : Seq.of(Tables.values()).map(Tables::getTableName)) {
          try {
            databaseReconciliator.reconcileRetention(context, database, retention, table)
                .stream()
                .forEach(output -> logger.info(
                    "Reconcile retention for database {} and table {}: {}",
                    database, table, output));
          } catch (Exception ex) {
            handleException(client, distributedLogs, cluster, ex);
            continue;
          }
        }
      }
      updateStatus(distributedLogs, database, retention);
    }
    String fluentdConfigHash = configReconciliator.getFluentdConfigHash();
    if (!Objects.equals(
        distributedLogs.getStatus().getFluentdConfigHash(),
        fluentdConfigHash)) {
      logger.info("Reloading fluentd configuration");
      configReconciliator.reloadFluentdConfiguration();
      distributedLogs.getStatus().setFluentdConfigHash(fluentdConfigHash);
    }
    distributedLogsScheduler.update(distributedLogs);
  }

  private boolean isPatroniReady(StackGresDistributedLogsContext context) {
    return context.getExistingResources().stream()
    .map(Tuple2::v1)
    .filter(Pod.class::isInstance)
    .map(Pod.class::cast)
    .anyMatch(pod -> pod.getMetadata().getName().equals(
        propertyContext.getString(DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME))
        && Optional.ofNullable(pod.getStatus())
        .map(PodStatus::getContainerStatuses)
        .filter(containerStatuses -> containerStatuses.stream()
            .anyMatch(containerStatus -> containerStatus.getName().equals(
                StackgresClusterContainers.PATRONI)
                && containerStatus.getReady()))
        .map(containersWithReadyPatroni -> true)
        .orElse(false));
  }

  private void updateStatus(StackGresDistributedLogs distributedLogs, String database,
      String retention) {
    Optional<StackGresDistributedLogsStatusDatabase> foundDistributedLogsDatabase =
        distributedLogs.getStatus().getDatabases()
        .stream()
        .filter(databaseStatus -> databaseStatus.getName().equals(database))
        .findAny();
    final StackGresDistributedLogsStatusDatabase distributedLogsDatabase =
        foundDistributedLogsDatabase.orElseGet(() -> new StackGresDistributedLogsStatusDatabase());
    if (!foundDistributedLogsDatabase.isPresent()) {
      distributedLogs.getStatus().getDatabases().add(distributedLogsDatabase);
    }
    distributedLogsDatabase.setName(database);
    distributedLogsDatabase.setRetention(retention);
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
    logger.error(message, ex);
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONTROLLER_ERROR,
        message + ": " + ex.getMessage(), distributedLogs, client);
  }

}
