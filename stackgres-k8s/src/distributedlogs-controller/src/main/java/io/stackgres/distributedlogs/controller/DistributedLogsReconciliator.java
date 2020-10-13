/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import com.google.common.collect.ImmutableMap;
import com.ongres.process.FluentProcess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.JdbcStatementTemplate;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusDatabase;
import io.stackgres.common.distributedlogs.Tables;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.distributedlogs.common.DistributedLogsEventReason;
import io.stackgres.distributedlogs.common.DistributedLogsProperty;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.reconciliation.Reconciliator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsReconciliator
    extends Reconciliator<StackGresDistributedLogsContext> {

  private static final String FLUENTD_CONF_FROM_CONFIGMAP_PATH = "/etc/fluentd/fluentd.conf";
  private static final String FLUENTD_CONF_PATH = "/fluentd/fluentd.conf";

  private final ResourceFinder<Secret> secretFinder;
  private final CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
  private final PostgresConnectionManager postgresConnectionManager;
  private final EventController eventController;
  private final JdbcStatementTemplate existsDatabaseTemplate;
  private final JdbcStatementTemplate createDatabaseTemplate;
  private final JdbcStatementTemplate updateRetentionTemplate;
  private final JdbcStatementTemplate reconcileRetentionTemplate;

  @Dependent
  public static class Parameters {
    @Inject ResourceFinder<Secret> secretFinder;
    @Inject CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
    @Inject PostgresConnectionManager postgresConnectionManager;
    @Inject EventController eventController;
  }

  @Inject
  public DistributedLogsReconciliator(Parameters parameters) {
    this.secretFinder = parameters.secretFinder;
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
    this.postgresConnectionManager = parameters.postgresConnectionManager;
    this.eventController = parameters.eventController;
    existsDatabaseTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsReconciliator.class.getResource("/exists-database.sql"));
    createDatabaseTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsReconciliator.class.getResource("/create-database.sql"));
    updateRetentionTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsReconciliator.class.getResource("/update-retention.sql"));
    reconcileRetentionTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsReconciliator.class.getResource("/reconcile-retention.sql"));
  }

  public static DistributedLogsReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsReconciliator(parameters.findAny().get());
  }

  @SuppressFBWarnings(value = {
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
        "REC_CATCH_EXCEPTION",
      },
      justification = "Seems false positives")
  @Override
  protected void reconcile(KubernetesClient client, StackGresDistributedLogsContext context)
      throws Exception {
    StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    if (distributedLogs.getStatus() == null
        || !context.getExistingResources().stream()
        .map(Tuple2::v1)
        .filter(Pod.class::isInstance)
        .map(Pod.class::cast)
        .anyMatch(pod -> pod.getMetadata().getName().equals(
            DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME.getString())
            && Optional.ofNullable(pod.getStatus())
            .map(PodStatus::getContainerStatuses)
            .filter(containerStatuses -> containerStatuses.stream()
                .anyMatch(containerStatus -> containerStatus.getName().equals(
                    StackgresClusterContainers.PATRONI)
                    && containerStatus.getReady()))
            .map(containersWithReadyPatroni -> true)
            .orElse(false))) {
      logger.warn("Waiting for distributedlogs cluster to become ready...");
      TimeUnit.SECONDS.sleep(1);
      return;
    }
    for (StackGresDistributedLogsStatusCluster cluster : distributedLogs
        .getStatus().getConnectedClusters()) {
      String database = FluentdUtil.databaseName(cluster.getNamespace(), cluster.getName());
      try (Connection connection = getConnection(context, "postgres")) {
        if (!existsDatabase(connection, database)) {
          logger.info("Creating database {}", database);
          createDatabase(connection, database);
        }
      } catch (Exception ex) {
        handleException(client, distributedLogs, cluster, ex);
      }
      try (Connection connection = getConnection(context, database)) {
        String retention = cluster.getConfig().getRetention();
        if (!Optional.of(distributedLogs.getStatus().getDatabases())
            .map(databases -> databases.get(database))
            .map(StackGresDistributedLogsStatusDatabase::getRetention)
            .map(currentRetention -> Objects.equals(retention, currentRetention))
            .orElse(true)) {
          for (String table : Seq.of(Tables.values()).map(Tables::getTableName)) {
            logger.info("Updating retention window for database {} and table to {}", database,
                retention);
            updateRetention(connection, table, retention);
          }
        }
        if (retention != null) {
          for (String table : Seq.of(Tables.values()).map(Tables::getTableName)) {
            reconcileRetention(connection, table, retention)
                .stream()
                .forEach(output -> logger.info(
                    "Reconcile retention for database {} and table {}: {}",
                    database, table, output));
          }
        }
        updateStatus(distributedLogs, database, retention);
      } catch (Exception ex) {
        handleException(client, distributedLogs, cluster, ex);
      }
    }
    String fluentdConfigHash = StackGresUtil.getMd5Sum(Paths.get(FLUENTD_CONF_FROM_CONFIGMAP_PATH));
    if (!Objects.equals(
        distributedLogs.getStatus().getFluentdConfigHash(),
        fluentdConfigHash)) {
      logger.info("Reloading fluentd configuration");
      reloadFluentdConfiguration();
      distributedLogs.getStatus().setFluentdConfigHash(fluentdConfigHash);
    }
    distributedLogsScheduler.update(distributedLogs);
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

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "A false positive bug check")
  private boolean existsDatabase(Connection connection, String database) throws SQLException {
    try (PreparedStatement existsDatabase = existsDatabaseTemplate
        .prepareStatement(connection)) {
      existsDatabaseTemplate.set(existsDatabase, "DATABASE", database);
      try (ResultSet resultSet = existsDatabase.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getBoolean(1);
        }
      }
    }
    throw new IllegalStateException("Can not check database existence");
  }

  private void createDatabase(Connection connection, String database) throws SQLException {
    try (PreparedStatement createDatabase = createDatabaseTemplate
        .prepareStatement(connection, ImmutableMap.of("DATABASE", database))) {
      createDatabase.execute();
    }
  }

  private void updateRetention(Connection connection, String table,
      String retention) throws SQLException {
    String effectiveRetention = Optional.ofNullable(retention).orElse("7 days");
    try (PreparedStatement updateRetention = updateRetentionTemplate
        .prepareStatement(connection)) {
      updateRetentionTemplate.set(updateRetention, "TABLE", table);
      updateRetentionTemplate.set(updateRetention, "RETENTION", effectiveRetention);
      updateRetention.execute();
    }
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "A false positive bug check")
  private List<String> reconcileRetention(Connection connection, String table,
      String retention) throws SQLException {
    String retentionUnit = retention.substring(retention.indexOf(" ") + 1);
    try (PreparedStatement reconcileRetention = reconcileRetentionTemplate
        .prepareStatement(connection)) {
      reconcileRetentionTemplate.set(reconcileRetention, "TABLE", table);
      reconcileRetentionTemplate.set(reconcileRetention, "RETENTION", retention);
      reconcileRetentionTemplate.set(reconcileRetention, "RETENTION_UNIT", retentionUnit);
      try (ResultSet resultSet = reconcileRetention.executeQuery()) {
        List<String> output = new ArrayList<>();
        while (resultSet.next()) {
          output.add(resultSet.getString(1));
        }
        return output;
      }
    }
  }

  private void updateStatus(StackGresDistributedLogs distributedLogs, String database,
      String retention) {
    StackGresDistributedLogsStatusDatabase distributedLogsDatabase =
        new StackGresDistributedLogsStatusDatabase();
    distributedLogsDatabase.setRetention(retention);
    distributedLogs.getStatus().getDatabases()
        .put(database, distributedLogsDatabase);
  }

  @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
      justification = "This is not a bug if working with containers")
  private void reloadFluentdConfiguration() throws IOException {
    List<String> oldConfigLines = Files.readAllLines(Paths.get(FLUENTD_CONF_PATH));
    boolean needsRestart = Files.readAllLines(Paths.get(FLUENTD_CONF_FROM_CONFIGMAP_PATH))
        .stream()
        .filter(configMapLine -> configMapLine.matches("^\\s*workers\\s+[0-9]+$"))
        .allMatch(configMapLine -> oldConfigLines
            .stream()
            .filter(line -> line.matches("^\\s*workers\\s+[0-9]+$"))
            .allMatch(line -> line.equals(configMapLine)));
    Files.copy(
        Paths.get(FLUENTD_CONF_FROM_CONFIGMAP_PATH),
        Paths.get(FLUENTD_CONF_PATH),
        StandardCopyOption.REPLACE_EXISTING);
    String fluentdPid = ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> command.startsWith("/usr/bin/ruby /usr/local/bin/fluentd "))
            .orElse(false))
        .map(process -> process.pid())
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Fluentd configmap not found"));
    if (needsRestart) {
      FluentProcess.start("kill", "-s", "SIGINT", fluentdPid).join();
    } else {
      FluentProcess.start("kill", "-s", "SIGUSR2", fluentdPid).join();
    }
  }

  private Connection getConnection(StackGresDistributedLogsContext context, String database)
      throws SQLException {
    final String name = context.getCluster().getMetadata().getName();
    final String namespace = context.getCluster().getMetadata().getNamespace();
    String serviceName = PatroniUtil.readWriteName(name);
    Secret secret = secretFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> new NotFoundException(
            "Secret with username and password for user postgres can not be found."));
    return postgresConnectionManager.getConnection(
        serviceName + "." + namespace,
        "postgres",
        ResourceUtil.decodeSecret(secret.getData().get("superuser-password")),
        database);
  }

}
