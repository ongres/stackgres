/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.YamlScalarWriter;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterSuperuserCredentialsTest {

  private static final List<String> PATRONI_CONFIG_LINES = List.of(
      "postgresql:",
      "  authentication: { superuser: { username: 'postgres2', password: 's2' },"
          + " replication: { username: 'replicator', password: 'r' } }");

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private PostgresConnectionManager postgresConnectionManager;

  @Mock
  private ClusterContext context;

  @Mock
  private Connection connection;

  private ClusterSuperuserCredentials clusterSuperuserCredentials;

  @BeforeEach
  void setUp() {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getCluster()).thenReturn(cluster);
    clusterSuperuserCredentials = new ClusterSuperuserCredentials(
        secretFinder, postgresConnectionManager,
        new PatroniConfigCredentials(new YamlMapperProvider(),
            new YamlScalarWriter(new YamlMapperProvider())));
  }

  @Test
  void get_withOnlyTheSecretUser_returnsItWithoutConnecting() {
    mockSecret(Map.of(
        StackGresPasswordKeys.SUPERUSER_USERNAME_KEY, "postgres",
        StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY, "s"));

    assertEquals(new PostgresUtil.Credentials("postgres", "s"),
        clusterSuperuserCredentials.get(context, List.of()));
    verifyNoInteractions(postgresConnectionManager);
  }

  @Test
  void get_withTheUserOfPatroniExisting_returnsIt() throws Exception {
    mockSecret(Map.of(
        StackGresPasswordKeys.SUPERUSER_USERNAME_KEY, "postgres3",
        StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY, "s3",
        StackGresPasswordKeys.SUPERUSER_PREVIOUS_USERNAME_KEY, "postgres2"));
    when(postgresConnectionManager.getUnixConnection(
        any(), anyInt(), any(), eq("postgres2"), any())).thenReturn(connection);

    assertEquals(new PostgresUtil.Credentials("postgres2", "s2"),
        clusterSuperuserCredentials.get(context, PATRONI_CONFIG_LINES));
  }

  @Test
  void get_withTheUserOfPatroniNotExisting_returnsTheUserOfTheSecret() throws Exception {
    mockSecret(Map.of(
        StackGresPasswordKeys.SUPERUSER_USERNAME_KEY, "postgres3",
        StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY, "s3"));
    when(postgresConnectionManager.getUnixConnection(
        any(), anyInt(), any(), eq("postgres2"), any()))
        .thenThrow(new SQLException("FATAL: role \"postgres2\" does not exist", "28000"));
    when(postgresConnectionManager.getUnixConnection(
        any(), anyInt(), any(), eq("postgres3"), any())).thenReturn(connection);

    assertEquals(new PostgresUtil.Credentials("postgres3", "s3"),
        clusterSuperuserCredentials.get(context, PATRONI_CONFIG_LINES));
  }

  @Test
  void get_withTheUserOfTheSecretNotExisting_returnsThePreviousUserOfTheSecret()
      throws Exception {
    mockSecret(Map.of(
        StackGresPasswordKeys.SUPERUSER_USERNAME_KEY, "postgres3",
        StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY, "s3",
        StackGresPasswordKeys.SUPERUSER_PREVIOUS_USERNAME_KEY, "postgres"));
    when(postgresConnectionManager.getUnixConnection(
        any(), anyInt(), any(), eq("postgres3"), any()))
        .thenThrow(new SQLException("FATAL: role \"postgres3\" does not exist", "28000"));
    when(postgresConnectionManager.getUnixConnection(
        any(), anyInt(), any(), eq("postgres"), any())).thenReturn(connection);

    assertEquals(new PostgresUtil.Credentials("postgres", "s3"),
        clusterSuperuserCredentials.get(context, List.of()));
  }

  @Test
  void get_withPostgresNotAvailable_returnsTheUserOfPatroni() throws Exception {
    mockSecret(Map.of(
        StackGresPasswordKeys.SUPERUSER_USERNAME_KEY, "postgres3",
        StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY, "s3"));
    when(postgresConnectionManager.getUnixConnection(
        any(), anyInt(), any(), eq("postgres2"), any()))
        .thenThrow(new SQLException("Connection refused", "08001"));

    assertEquals(new PostgresUtil.Credentials("postgres2", "s2"),
        clusterSuperuserCredentials.get(context, PATRONI_CONFIG_LINES));
  }

  private void mockSecret(Map<String, String> data) {
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(ResourceUtil.encodeSecret(data))
            .build()));
  }

}
