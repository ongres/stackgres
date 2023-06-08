/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.crd.sgscript.StackGresScriptTransactionIsolationLevel.fromString;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.crd.sgscript.StackGresScriptTransactionIsolationLevel;
import io.stackgres.common.postgres.PostgresConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ManagedSqlScriptEntryExecutor {

  static final String IS_MANAGED_SQL_STATUS_TABLE_MISSING_QUERY =
      "SELECT NOT EXISTS (SELECT * FROM pg_tables"
          + " WHERE (schemaname, tablename) = ('managed_sql', 'status'))";

  static final String CREATE_MANAGED_SQL_SCHEMA_STATEMENT =
      "CREATE SCHEMA IF NOT EXISTS managed_sql";

  static final String CREATE_MANAGED_SQL_STATUS_TABLE_STATEMENT =
      "CREATE TABLE IF NOT EXISTS managed_sql.status"
          + " (id int, entry_id int, version int,"
          + " hash text, applied timestamp DEFAULT now())";

  static final String GRANT_ON_SCHEMA_MANAGED_SQL_STATEMENT =
      "GRANT USAGE ON SCHEMA managed_sql TO PUBLIC";

  static final String GRANT_ON_MANAGED_SQL_STATUS_TABLE_STATEMENT =
      "GRANT SELECT, INSERT, DELETE ON managed_sql.status TO PUBLIC";

  static final String FIND_APPLIED_TIMESTAMP_FOR_SCRIP_ENTRY_QUERY =
      "SELECT applied FROM managed_sql.status"
          + " WHERE (id, entry_id, version, hash) = (?, ?, ?, ?)";

  static final String DELETE_SCRIPT_ENTRY_STATEMENT =
      "DELETE FROM managed_sql.status WHERE (id, entry_id) = (?, ?)";

  static final String INSERT_SCRIPT_ENTRY_STATEMENT =
      "INSERT INTO managed_sql.status (id, entry_id, version, hash) VALUES (?, ?, ?, ?)";

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ManagedSqlScriptEntryExecutor.class);

  private final PostgresConnectionManager postgresConnectionManager;

  @Inject
  public ManagedSqlScriptEntryExecutor(PostgresConnectionManager postgresConnectionManager) {
    this.postgresConnectionManager = postgresConnectionManager;
  }

  protected void executeScriptEntry(
      ManagedSqlScriptEntry scriptEntry, String sql)
      throws SQLException {
    if (scriptEntry.getScriptEntry().getWrapInTransaction() == null) {
      LOGGER.info("Executing managed script {} with no transaction",
          scriptEntry.getManagedScriptEntryDescription());
      executeScriptEntryWithoutTransaction(scriptEntry, sql);
    } else {
      StackGresScriptTransactionIsolationLevel transactionIsolationLevel =
          fromString(scriptEntry.getScriptEntry().getWrapInTransaction());
      if (scriptEntry.getScriptEntry().getStoreStatusInDatabaseOrDefault()) {
        LOGGER.info("Executing managed script {} and store status wrapped in a transaction with"
            + " isolation level {}",
            scriptEntry.getManagedScriptEntryDescription(),
            transactionIsolationLevel.toSqlString());
        executeScriptEntryAndStoreStatusInTransaction(scriptEntry, transactionIsolationLevel, sql);
      } else {
        LOGGER.info("Executing managed script {} wrapped in a transaction with isolation level {}",
            scriptEntry.getManagedScriptEntryDescription(),
            transactionIsolationLevel.toSqlString());
        executeScriptEntryInTransaction(scriptEntry, transactionIsolationLevel, sql);
      }
    }
  }

  private void executeScriptEntryWithoutTransaction(
      ManagedSqlScriptEntry scriptEntry, String sql)
      throws SQLException {
    try (Connection connection = getConnection(
        scriptEntry.getScriptEntry().getDatabaseOrDefault(),
        scriptEntry.getScriptEntry().getUserOrDefault())) {
      try (var statement = connection.createStatement()) {
        statement.execute(sql);
      }
    }
  }

  private void executeScriptEntryInTransaction(ManagedSqlScriptEntry scriptEntry,
      StackGresScriptTransactionIsolationLevel transactionIsolationLevel, String sql)
      throws SQLException {
    try (Connection connection = getConnection(
        scriptEntry.getScriptEntry().getDatabaseOrDefault(),
        scriptEntry.getScriptEntry().getUserOrDefault())) {
      connection.setAutoCommit(false);
      connection.setTransactionIsolation(transactionIsolationLevel.toJdbcConstant());
      try {
        try (var statement = connection.createStatement()) {
          statement.execute(sql);
        }
        connection.commit();
      } catch (SQLException | RuntimeException ex) {
        connection.rollback();
        throw ex;
      }
    }
  }

  private void executeScriptEntryAndStoreStatusInTransaction(ManagedSqlScriptEntry scriptEntry,
      StackGresScriptTransactionIsolationLevel transactionIsolationLevel, String sql)
      throws SQLException {
    try (Connection connection = getConnection(
        scriptEntry.getScriptEntry().getDatabaseOrDefault(),
        SUPERUSER_USERNAME)) {
      connection.setAutoCommit(false);
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      try {
        boolean managedSqlStatusTableMissing = isManagedSqlStatusTableMissing(connection);
        if (managedSqlStatusTableMissing) {
          createManagedSqlStatusTable(connection);
        }
        connection.commit();
      } catch (SQLException | RuntimeException ex) {
        connection.rollback();
        throw ex;
      }
    }

    try (Connection connection = getConnection(
        scriptEntry.getScriptEntry().getDatabaseOrDefault(),
        scriptEntry.getScriptEntry().getUserOrDefault())) {
      connection.setAutoCommit(false);
      connection.setTransactionIsolation(transactionIsolationLevel.toJdbcConstant());
      try {
        var foundScriptAppliedTimestamp = findScriptAppliedTimestamp(scriptEntry, connection);
        if (foundScriptAppliedTimestamp.isPresent()) {
          LOGGER.warn("Script {} was already applied at timestamp {}, skipping execution",
              scriptEntry.getManagedScriptEntryDescription(),
              foundScriptAppliedTimestamp.orElseThrow());
          return;
        }
        try (var statement = connection.createStatement()) {
          statement.execute(sql);
        }
        updateManagedSqlStatusTable(scriptEntry, connection);
        connection.commit();
      } catch (SQLException | RuntimeException ex) {
        connection.rollback();
        throw ex;
      }
    }
  }

  private boolean isManagedSqlStatusTableMissing(Connection connection) throws SQLException {
    boolean managedSqlTableMissing = true;
    try (var statement = connection.prepareStatement(
        IS_MANAGED_SQL_STATUS_TABLE_MISSING_QUERY)) {
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          managedSqlTableMissing = resultSet.getBoolean(1);
        }
      }
    }
    return managedSqlTableMissing;
  }

  private void createManagedSqlStatusTable(Connection connection) throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.execute(CREATE_MANAGED_SQL_SCHEMA_STATEMENT);
    }
    try (var statement = connection.createStatement()) {
      statement.execute(CREATE_MANAGED_SQL_STATUS_TABLE_STATEMENT);
    }
    try (var statement = connection.createStatement()) {
      statement.execute(GRANT_ON_SCHEMA_MANAGED_SQL_STATEMENT);
    }
    try (var statement = connection.createStatement()) {
      statement.execute(GRANT_ON_MANAGED_SQL_STATUS_TABLE_STATEMENT);
    }
  }

  private Optional<Instant> findScriptAppliedTimestamp(ManagedSqlScriptEntry scriptEntry,
      Connection connection) throws SQLException {
    try (var statement = connection.prepareStatement(
        FIND_APPLIED_TIMESTAMP_FOR_SCRIP_ENTRY_QUERY)) {
      statement.setInt(1, scriptEntry.getManagedScript().getId());
      statement.setInt(2, scriptEntry.getScriptEntry().getId());
      statement.setInt(3, scriptEntry.getScriptEntry().getVersion());
      statement.setString(4, scriptEntry.getScriptEntryStatus().getHash());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(resultSet.getTimestamp(1).toInstant());
        }
      }
    }
    return Optional.empty();
  }

  private void updateManagedSqlStatusTable(ManagedSqlScriptEntry scriptEntry, Connection connection)
      throws SQLException {
    try (var statement = connection.prepareStatement(
        DELETE_SCRIPT_ENTRY_STATEMENT)) {
      statement.setInt(1, scriptEntry.getManagedScript().getId());
      statement.setInt(2, scriptEntry.getScriptEntry().getId());
      statement.execute();
    }
    try (var statement = connection.prepareStatement(
        INSERT_SCRIPT_ENTRY_STATEMENT)) {
      statement.setInt(1, scriptEntry.getManagedScript().getId());
      statement.setInt(2, scriptEntry.getScriptEntry().getId());
      statement.setInt(3, scriptEntry.getScriptEntry().getVersion());
      statement.setString(4, scriptEntry.getScriptEntryStatus().getHash());
      statement.execute();
    }
  }

  protected Connection getConnection(String database, String user)
      throws SQLException {
    return postgresConnectionManager.getUnixConnection(
        ClusterStatefulSetPath.PG_RUN_PATH.path(), EnvoyUtil.PG_PORT,
        database,
        user,
        "");
  }

}
