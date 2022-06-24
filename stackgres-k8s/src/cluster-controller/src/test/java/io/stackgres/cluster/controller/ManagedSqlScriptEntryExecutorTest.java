/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.CREATE_MANAGED_SQL_SCHEMA_STATEMENT;
import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.CREATE_MANAGED_SQL_STATUS_TABLE_STATEMENT;
import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.DELETE_SCRIPT_ENTRY_STATEMENT;
import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.FIND_APPLIED_TIMESTAMP_FOR_SCRIP_ENTRY_QUERY;
import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.GRANT_ON_MANAGED_SQL_STATUS_TABLE_STATEMENT;
import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.GRANT_ON_SCHEMA_MANAGED_SQL_STATEMENT;
import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.INSERT_SCRIPT_ENTRY_STATEMENT;
import static io.stackgres.cluster.controller.ManagedSqlScriptEntryExecutor.IS_MANAGED_SQL_STATUS_TABLE_MISSING_QUERY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagedSqlScriptEntryExecutorTest {

  @Mock
  PostgresConnectionManager postgresConnectionManager;
  @Mock
  Connection connection;
  @Mock
  PreparedStatement statement;
  @Mock
  ResultSet resultSet;

  private ManagedSqlScriptEntry scriptEntry;

  private ManagedSqlScriptEntryExecutor scriptEntryExecutor;

  @BeforeEach
  void setUp() throws Exception {
    StackGresCluster cluster = JsonUtil
        .readFromJson("stackgres_cluster/managed_sql.json",
            StackGresCluster.class);
    StackGresScript script = JsonUtil
        .readFromJson("stackgres_script/default.json",
            StackGresScript.class);
    scriptEntry = ImmutableManagedSqlScriptEntry.builder()
        .managedSqlStatus(cluster.getStatus().getManagedSql())
        .managedScript(cluster.getSpec().getManagedSql().getScripts().get(0))
        .managedScriptStatus(cluster.getStatus().getManagedSql().getScripts().get(0))
        .script(script)
        .scriptEntry(script.getSpec().getScripts().get(1))
        .scriptEntryStatus(script.getStatus().getScripts().get(1))
        .build();
    scriptEntry.getScriptEntryStatus().setHash("test");

    scriptEntryExecutor = new ManagedSqlScriptEntryExecutor(postgresConnectionManager);
  }

  @Test
  void testExecutorWithStatusWhenStatusTableIsMissing_createStatusTableAndApplyScript()
      throws Exception {
    when(postgresConnectionManager.getUnixConnection(any(), anyInt(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.prepareStatement(any())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getBoolean(anyInt())).thenReturn(true);

    scriptEntryExecutor.executeScriptEntry(scriptEntry, "CREATE TABLE test");

    verify(postgresConnectionManager, times(2)).getUnixConnection(
        any(), anyInt(), any(), any(), any());
    verify(connection, times(5)).createStatement();
    verify(connection, times(4)).prepareStatement(any());
    verify(statement, times(5)).execute(any());
    verify(statement, times(2)).execute();
    verify(statement, times(2)).executeQuery();

    InOrder inOrder = inOrder(postgresConnectionManager, connection, statement, resultSet);
    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));

    inOrder.verify(connection).prepareStatement(IS_MANAGED_SQL_STATUS_TABLE_MISSING_QUERY);
    inOrder.verify(statement).executeQuery();
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet).getBoolean(1);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute(CREATE_MANAGED_SQL_SCHEMA_STATEMENT);
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute(CREATE_MANAGED_SQL_STATUS_TABLE_STATEMENT);
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute(GRANT_ON_SCHEMA_MANAGED_SQL_STATEMENT);
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement)
        .execute(GRANT_ON_MANAGED_SQL_STATUS_TABLE_STATEMENT);

    inOrder.verify(connection).close();

    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));
    inOrder.verify(connection).setAutoCommit(false);
    inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    inOrder.verify(connection)
        .prepareStatement(FIND_APPLIED_TIMESTAMP_FOR_SCRIP_ENTRY_QUERY);
    inOrder.verify(statement).setInt(1, 0);
    inOrder.verify(statement).setInt(2, 1);
    inOrder.verify(statement).setInt(3, 0);
    inOrder.verify(statement).setString(4, "test");
    inOrder.verify(statement).executeQuery();
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet, times(0)).getTimestamp(1);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute("CREATE TABLE test");
    inOrder.verify(statement).close();

    inOrder.verify(connection)
        .prepareStatement(DELETE_SCRIPT_ENTRY_STATEMENT);
    inOrder.verify(statement).setInt(1, 0);
    inOrder.verify(statement).setInt(2, 1);
    inOrder.verify(statement).execute();
    inOrder.verify(statement).close();

    inOrder.verify(connection)
        .prepareStatement(INSERT_SCRIPT_ENTRY_STATEMENT);
    inOrder.verify(statement).setInt(1, 0);
    inOrder.verify(statement).setInt(2, 1);
    inOrder.verify(statement).setInt(3, 0);
    inOrder.verify(statement).setString(4, "test");
    inOrder.verify(statement).execute();
    inOrder.verify(statement).close();

    inOrder.verify(connection).commit();
    inOrder.verify(connection).close();
  }

  @Test
  void testExecutorWithStatusWhenScriptAlreadyApplied_doNotApplyScript() throws Exception {
    when(postgresConnectionManager.getUnixConnection(any(), anyInt(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.prepareStatement(any())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true).thenReturn(true);
    when(resultSet.getBoolean(anyInt())).thenReturn(false);
    when(resultSet.getTimestamp(anyInt())).thenReturn(new Timestamp(System.currentTimeMillis()));

    scriptEntryExecutor.executeScriptEntry(scriptEntry, "CREATE TABLE test");

    verify(postgresConnectionManager, times(2)).getUnixConnection(
        any(), anyInt(), any(), any(), any());
    verify(connection, times(0)).createStatement();
    verify(connection, times(2)).prepareStatement(any());
    verify(statement, times(0)).execute(any());
    verify(statement, times(0)).execute();
    verify(statement, times(2)).executeQuery();

    InOrder inOrder = inOrder(postgresConnectionManager, connection, statement, resultSet);
    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));

    inOrder.verify(connection).prepareStatement(IS_MANAGED_SQL_STATUS_TABLE_MISSING_QUERY);
    inOrder.verify(statement).executeQuery();
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet).getBoolean(1);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();

    inOrder.verify(connection).close();

    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));
    inOrder.verify(connection).setAutoCommit(false);
    inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    inOrder.verify(connection)
        .prepareStatement(FIND_APPLIED_TIMESTAMP_FOR_SCRIP_ENTRY_QUERY);
    inOrder.verify(statement).setInt(1, 0);
    inOrder.verify(statement).setInt(2, 1);
    inOrder.verify(statement).setInt(3, 0);
    inOrder.verify(statement).setString(4, "test");
    inOrder.verify(statement).executeQuery();
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet).getTimestamp(1);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();

    inOrder.verify(connection).close();
  }

  @Test
  void testExecutorWithStatusWhenStatusTableCreationFail_rollbackItsCreation() throws Exception {
    when(postgresConnectionManager.getUnixConnection(any(), anyInt(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.prepareStatement(any())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getBoolean(anyInt())).thenReturn(true);
    when(statement.execute(any()))
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(true)
        .thenThrow(new SQLException("test"));

    assertThrows(SQLException.class, () -> scriptEntryExecutor
        .executeScriptEntry(scriptEntry, "CREATE TABLE test"));

    verify(postgresConnectionManager, times(1)).getUnixConnection(
        any(), anyInt(), any(), any(), any());
    verify(connection, times(4)).createStatement();
    verify(connection, times(1)).prepareStatement(any());
    verify(statement, times(4)).execute(any());
    verify(statement, times(0)).execute();
    verify(statement, times(1)).executeQuery();

    InOrder inOrder = inOrder(postgresConnectionManager, connection, statement, resultSet);
    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));

    inOrder.verify(connection).prepareStatement(IS_MANAGED_SQL_STATUS_TABLE_MISSING_QUERY);
    inOrder.verify(statement).executeQuery();
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet).getBoolean(1);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute(CREATE_MANAGED_SQL_SCHEMA_STATEMENT);
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute(CREATE_MANAGED_SQL_STATUS_TABLE_STATEMENT);
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute(GRANT_ON_SCHEMA_MANAGED_SQL_STATEMENT);
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement)
        .execute(GRANT_ON_MANAGED_SQL_STATUS_TABLE_STATEMENT);

    inOrder.verify(connection).close();
  }

  @Test
  void testExecutorWithStatusWhenSqlFail_rollbackItsApplication() throws Exception {
    when(postgresConnectionManager.getUnixConnection(any(), anyInt(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.prepareStatement(any())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true).thenReturn(false);
    when(resultSet.getBoolean(anyInt())).thenReturn(false);
    when(statement.execute(any()))
        .thenThrow(new SQLException("test"));

    assertThrows(SQLException.class, () -> scriptEntryExecutor
        .executeScriptEntry(scriptEntry, "CREATE TABLE test"));

    verify(postgresConnectionManager, times(2)).getUnixConnection(
        any(), anyInt(), any(), any(), any());
    verify(connection, times(1)).createStatement();
    verify(connection, times(2)).prepareStatement(any());
    verify(statement, times(1)).execute(any());
    verify(statement, times(0)).execute();
    verify(statement, times(2)).executeQuery();

    InOrder inOrder = inOrder(postgresConnectionManager, connection, statement, resultSet);
    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));

    inOrder.verify(connection).prepareStatement(IS_MANAGED_SQL_STATUS_TABLE_MISSING_QUERY);
    inOrder.verify(statement).executeQuery();
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet).getBoolean(1);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();

    inOrder.verify(connection).close();

    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));
    inOrder.verify(connection).setAutoCommit(false);
    inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    inOrder.verify(connection)
        .prepareStatement(FIND_APPLIED_TIMESTAMP_FOR_SCRIP_ENTRY_QUERY);
    inOrder.verify(statement).setInt(1, 0);
    inOrder.verify(statement).setInt(2, 1);
    inOrder.verify(statement).setInt(3, 0);
    inOrder.verify(statement).setString(4, "test");
    inOrder.verify(statement).executeQuery();
    inOrder.verify(resultSet).next();
    inOrder.verify(resultSet, times(0)).getTimestamp(1);
    inOrder.verify(resultSet).close();
    inOrder.verify(statement).close();

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute("CREATE TABLE test");
    inOrder.verify(statement).close();

    inOrder.verify(connection).rollback();
    inOrder.verify(connection).close();
  }

  @Test
  void testExecutorWrappedInTransaction_executeScriptInTransaction() throws Exception {
    scriptEntry.getScriptEntry().setStoreStatusInDatabase(false);
    when(postgresConnectionManager.getUnixConnection(any(), anyInt(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);

    scriptEntryExecutor.executeScriptEntry(scriptEntry, "CREATE TABLE test");

    verify(postgresConnectionManager, times(1)).getUnixConnection(
        any(), anyInt(), any(), any(), any());
    verify(connection, times(1)).createStatement();
    verify(statement, times(1)).execute(any());

    InOrder inOrder = inOrder(postgresConnectionManager, connection, statement, resultSet);
    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));
    inOrder.verify(connection).setAutoCommit(false);
    inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute("CREATE TABLE test");
    inOrder.verify(statement).close();

    inOrder.verify(connection).commit();
    inOrder.verify(connection).close();
  }

  @Test
  void testExecutorWrappedInTransactionWithFailingScript_executeScriptInTransactionAndRollback()
      throws Exception {
    scriptEntry.getScriptEntry().setStoreStatusInDatabase(false);
    when(postgresConnectionManager.getUnixConnection(any(), anyInt(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.execute(any())).thenThrow(new SQLException("test"));

    assertThrows(SQLException.class,
        () -> scriptEntryExecutor.executeScriptEntry(scriptEntry, "CREATE TABLE test"));

    verify(postgresConnectionManager, times(1)).getUnixConnection(
        any(), anyInt(), any(), any(), any());
    verify(connection, times(1)).createStatement();
    verify(statement, times(1)).execute(any());

    InOrder inOrder = inOrder(postgresConnectionManager, connection, statement, resultSet);
    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));
    inOrder.verify(connection).setAutoCommit(false);
    inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute("CREATE TABLE test");
    inOrder.verify(statement).close();

    inOrder.verify(connection).rollback();
    inOrder.verify(connection).close();
  }

  @Test
  void testExecutor_executeScript() throws Exception {
    scriptEntry.getScriptEntry().setWrapInTransaction(null);
    scriptEntry.getScriptEntry().setStoreStatusInDatabase(false);
    when(postgresConnectionManager.getUnixConnection(any(), anyInt(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);

    scriptEntryExecutor.executeScriptEntry(scriptEntry, "CREATE TABLE test");

    verify(postgresConnectionManager, times(1)).getUnixConnection(
        any(), anyInt(), any(), any(), any());
    verify(connection, times(1)).createStatement();
    verify(statement, times(1)).execute(any());

    InOrder inOrder = inOrder(postgresConnectionManager, connection, statement, resultSet);
    inOrder.verify(postgresConnectionManager)
        .getUnixConnection(any(), anyInt(),
            eq("postgres"),
            eq("postgres"),
            eq(""));

    inOrder.verify(connection).createStatement();
    inOrder.verify(statement).execute("CREATE TABLE test");
    inOrder.verify(statement).close();

    inOrder.verify(connection).close();
  }

}
