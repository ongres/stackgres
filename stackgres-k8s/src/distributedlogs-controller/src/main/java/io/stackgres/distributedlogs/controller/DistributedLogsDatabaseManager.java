/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.JdbcStatementTemplate;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DistributedLogsDatabaseManager {

  private final ResourceFinder<Secret> secretFinder;
  private final PostgresConnectionManager postgresConnectionManager;
  private final JdbcStatementTemplate existsDatabaseTemplate;
  private final JdbcStatementTemplate createDatabaseTemplate;
  private final JdbcStatementTemplate updateRetentionTemplate;
  private final JdbcStatementTemplate reconcileRetentionTemplate;

  @Dependent
  public static class Parameters {
    @Inject ResourceFinder<Secret> secretFinder;
    @Inject PostgresConnectionManager postgresConnectionManager;
  }

  @Inject
  public DistributedLogsDatabaseManager(Parameters parameters) {
    this.secretFinder = parameters.secretFinder;
    this.postgresConnectionManager = parameters.postgresConnectionManager;
    existsDatabaseTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsDatabaseManager.class.getResource("/exists-database.sql"));
    createDatabaseTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsDatabaseManager.class.getResource("/create-database.sql"));
    updateRetentionTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsDatabaseManager.class.getResource("/update-retention.sql"));
    reconcileRetentionTemplate = JdbcStatementTemplate.fromResource(
        DistributedLogsDatabaseManager.class.getResource("/reconcile-retention.sql"));
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "False positive")
  public boolean existsDatabase(StackGresDistributedLogsContext context, String database)
      throws SQLException {
    try (Connection connection = getConnection(context, "postgres")) {
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
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "False positive")
  public void createDatabase(StackGresDistributedLogsContext context, String database)
      throws SQLException {
    try (Connection connection = getConnection(context, "postgres")) {
      try (PreparedStatement createDatabase = createDatabaseTemplate
          .prepareStatement(connection, ImmutableMap.of("DATABASE", database))) {
        createDatabase.execute();
      }
    }
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "False positive")
  public void updateRetention(StackGresDistributedLogsContext context, String database,
      String retention, String table) throws SQLException {
    try (Connection connection = getConnection(context, database)) {
      String effectiveRetention = Optional.ofNullable(retention).orElse("7 days");
      try (PreparedStatement updateRetention = updateRetentionTemplate
          .prepareStatement(connection)) {
        updateRetentionTemplate.set(updateRetention, "TABLE", table);
        updateRetentionTemplate.set(updateRetention, "RETENTION", effectiveRetention);
        updateRetention.execute();
      }
    }
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "False positive")
  public List<String> reconcileRetention(StackGresDistributedLogsContext context, String database,
      String retention, String table) throws SQLException {
    try (Connection connection = getConnection(context, database)) {
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
  }

  private Connection getConnection(StackGresDistributedLogsContext context, String database)
      throws SQLException {
    final String name = context.getCluster().getMetadata().getName();
    final String namespace = context.getCluster().getMetadata().getNamespace();
    String serviceName = PatroniUtil.readWriteName(context.getCluster());
    Secret secret = secretFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> new NotFoundException(
            "Secret with username and password for user postgres can not be found."));
    return postgresConnectionManager.getConnection(
        serviceName + "." + namespace, EnvoyUtil.PG_PORT,
        database,
        ResourceUtil.decodeSecret(secret.getData()
            .get(SUPERUSER_USERNAME_KEY)),
        ResourceUtil.decodeSecret(secret.getData()
            .get(SUPERUSER_PASSWORD_KEY)));
  }

}
