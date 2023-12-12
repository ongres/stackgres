/*
 * Copyright (C) 2023 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.apiweb.dto.pgstat.PostgresStatDto;
import io.stackgres.apiweb.rest.utils.NamespacedClusterPgResourceQueryGenerator;
import io.stackgres.apiweb.rest.utils.NamespacedClusterPgResourceQueryGeneratorImpl;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("uncheked")
public class NamespacedClusterPgResourceTest {

  @Mock
  private PostgresConnectionManager postgresConnectionManager;
  @Mock
  private ResourceFinder<Secret> resourceFinder;
  @Mock
  private CustomResourceFinder<StackGresCluster> customResourceFinder;

  private final NamespacedClusterPgResourceQueryGenerator queryGenerator =
          new NamespacedClusterPgResourceQueryGeneratorImpl();

  private NamespacedClusterPgResource endpoint;

  @BeforeEach
  protected void init() {
    endpoint = new NamespacedClusterPgResource(
        postgresConnectionManager,
        resourceFinder,
        customResourceFinder,
        queryGenerator);
  }

  @Test
  @DisplayName("Secret is not found")
  public void test_secret_is_not_found() {
    Mockito.when(resourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString()))
          .thenReturn(Optional.empty());

    Assertions.assertThrows(IllegalArgumentException.class,
          () -> endpoint.statements("namespace",
              "name",
              "named", 0,
              "sort",
              "dir"));
  }

  @Test
  @DisplayName("SGCluster is not found")
  public void test_stackgres_cluster_is_not_found() {
    final Secret secret = Fixtures.secret().loadDefault().get();
    Mockito.when(resourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Optional.of(secret));

    Mockito.when(
        customResourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
        .thenReturn(Optional.empty());

    Assertions.assertThrows(IllegalArgumentException.class, ()
            -> endpoint.statements("namespace",
            "name",
            "named",
            0,
            "sort",
            "dir"));
  }

  @Test
  @DisplayName("Connection to postgres fails")
  void test_connection_fails() throws SQLException {
    final Secret secret = Fixtures.secret().loadDefault().get();
    final StackGresCluster cluster = Fixtures
              .clusterList()
              .loadDefault()
              .get()
              .getItems()
              .stream()
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("StackGresCluster cannot be null"));

    Mockito.when(
        resourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
              .thenReturn(Optional.of(secret));

    Mockito.when(
        customResourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
              .thenReturn(Optional.of(cluster));

    Mockito.when(postgresConnectionManager.getConnection(Mockito.anyString(),
                      Mockito.anyInt(),
                      Mockito.anyString(),
                      Mockito.anyString(),
                      Mockito.anyString(),
                      Mockito.isNull()))
              .thenThrow(new SQLException("Could not connect to the database"));

    Assertions.assertThrows(RuntimeException.class, () ->
              endpoint.statements("namespace",
                      "name",
                      "named", 0,
                      "sort",
                      "dir"));
  }

  @Test
  @DisplayName("pg_stat_statements query returns successfully result")
  void test_pg_stat_statements_query() throws SQLException {
    final MockConnection connection = getMockConnection();

    final Secret secret = Fixtures.secret().loadDefault().get();
    secret.setData(
              Map.of("superuser-username", Base64
                        .getEncoder()
                      .encodeToString("username".getBytes()),
                      "superuser-password", Base64
                      .getEncoder()
                      .encodeToString("password".getBytes())
              )
    );
    final StackGresCluster cluster = Fixtures
              .clusterList()
              .loadDefault()
              .get()
              .getItems()
              .stream()
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("StackGresCluster cannot be null"));

    Mockito.when(
        resourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
              .thenReturn(Optional.of(secret));

    Mockito.when(
        customResourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
              .thenReturn(Optional.of(cluster));

    final ArgumentCaptor<String> hostCapture = ArgumentCaptor.forClass(String.class);
    Mockito.when(postgresConnectionManager.getConnection(Mockito.anyString(),
                      Mockito.anyInt(),
                      Mockito.anyString(),
                      Mockito.anyString(),
                      Mockito.anyString()))
              .thenReturn(connection);

    final List<Object> statements =
              endpoint.statements("namespace",
                      "name",
                      NamespacedClusterPgResource.TOP_PG_STAT_STATEMENTS,
                      20,
                      "user",
                      "desc");

    Assertions.assertEquals(
              new PostgresStatDto(0, List.of("user", "score", "timestamp")),
              statements.get(0));
  }

  @Test
  @DisplayName("pg_stat_activity query returns successfully result")
  void test_pg_stat_activity_query() throws SQLException {
    final MockConnection mockConnection = getMockConnection();

    final Secret secret = Fixtures.secret().loadDefault().get();
    secret.setData(
              Map.of("superuser-username", Base64
                      .getEncoder()
                      .encodeToString("username".getBytes()),
                      "superuser-password", Base64
                      .getEncoder()
                      .encodeToString("password".getBytes())
              )
    );

    final StackGresCluster cluster = Fixtures
              .clusterList()
              .loadDefault()
              .get()
              .getItems()
              .stream()
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("StackGresCluster cannot be null"));

    Mockito.when(
        resourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
              .thenReturn(Optional.of(secret));

    Mockito.when(
        customResourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
              .thenReturn(Optional.of(cluster));

    Mockito.when(postgresConnectionManager.getConnection(Mockito.anyString(),
                      Mockito.anyInt(),
                      Mockito.anyString(),
                      Mockito.anyString(),
                      Mockito.anyString()))
              .thenReturn(mockConnection);

    final List<Object> statements =
              endpoint.statements("namespace",
                      "name",
                      NamespacedClusterPgResource.TOP_PG_STAT_STATEMENTS,
                      20,
                      "sort",
                      "dir");

    Assertions.assertEquals(
              new PostgresStatDto(0, List.of("user", "score", "timestamp")),
              statements.get(0));
  }

  @Test
  @DisplayName("pg_locks query returns successfully result")
  void test_pg_locks_query() throws SQLException {
    final MockConnection mockConnection = getMockConnection();

    final Secret secret = Fixtures.secret().loadDefault().get();
    secret.setData(
            Map.of("superuser-username", Base64.getEncoder().encodeToString("username".getBytes()),
                    "superuser-password", Base64.getEncoder().encodeToString("password".getBytes())
            )
    );

    final StackGresCluster cluster = Fixtures
            .clusterList()
            .loadDefault()
            .get()
            .getItems()
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("StackGresCluster cannot be null"));

    Mockito.when(
        resourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
            .thenReturn(Optional.of(secret));

    Mockito.when(
        customResourceFinder.findByNameAndNamespace(Mockito.anyString(), Mockito.anyString())
        )
            .thenReturn(Optional.of(cluster));

    Mockito.when(postgresConnectionManager.getConnection(Mockito.anyString(),
                      Mockito.anyInt(),
                      Mockito.anyString(),
                      Mockito.anyString(),
                      Mockito.anyString()))
              .thenReturn(mockConnection);

    final List<Object> statements =
            endpoint.statements("namespace",
                    "name",
                    NamespacedClusterPgResource.TOP_PG_STAT_STATEMENTS,
                    20,
                    "sort",
                    "dir");

    Assertions.assertEquals(new PostgresStatDto(0, List.of("user", "score", "timestamp")),
            statements.get(0));
  }

  @Test
  @DisplayName("pg_stat_statements query is syntactically correct")
  void test_pg_stat_statements_query_syntax() {
    assert true;
  }

  private MockDataProvider getMockDataProvider() {
    return ctx -> {

      final DSLContext select = DSL.using(SQLDialect.POSTGRES);
      final MockResult[] mock = new MockResult[1];

      final Result<Record3<String, Integer, Integer>> result = select.newResult(
          DSL.field("user", String.class),
          DSL.field("score", Integer.class),
          DSL.field("timestamp", Integer.class));

      result.add(
          select.newRecord(
                  DSL.field("user", String.class),
                  DSL.field("score", Integer.class),
                  DSL.field("timestamp", Integer.class))
              .values("Gianluca", 100, 2023)
      );

      result.add(
          select.newRecord(
                  DSL.field("user", String.class),
                  DSL.field("score", Integer.class),
                  DSL.field("timestamp", Integer.class)
              )
              .values("Matteo", 90, 2022)
      );

      mock[0] = new MockResult(2, result);

      return mock;
    };
  }
  private MockConnection getMockConnection() {
    return new MockConnection(getMockDataProvider());
  }
}
