/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PgBouncerAuthFileReconciliatorTest {

  @Mock
  private CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private PostgresConnectionManager postgresConnectionManager;

  @Mock
  private FileSystemHandler fileSystemHandler;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement preparedStatement;

  @Mock
  private ResultSet resultSet;

  @Mock
  private ClusterContext context;

  private PgBouncerAuthFileReconciliator reconciliator;

  @BeforeEach
  void setUp() throws Exception {
    reconciliator = new PgBouncerAuthFileReconciliator(
        poolingConfigFinder, secretFinder, postgresConnectionManager, fileSystemHandler);
  }

  @Test
  void testReconciliationWithoutUsers_authFileIsUpdated() throws Exception {
    StackGresCluster cluster = JsonUtil
        .readFromJson("stackgres_cluster/list.json",
            StackGresClusterList.class)
        .getItems().get(0);
    StackGresPoolingConfig poolingConfig = JsonUtil
        .readFromJson("pooling_config/default.json",
            StackGresPoolingConfig.class);
    poolingConfig.getSpec().getPgBouncer().getPgbouncerIni().setUsers(ImmutableMap.of());
    Secret secret = JsonUtil
        .readFromJson("secret/patroni.json",
            Secret.class);
    when(context.getCluster()).thenReturn(cluster);
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(poolingConfig));
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(secret));
    when(postgresConnectionManager.getConnection(any(), any(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.prepareStatement(any()))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery())
        .thenReturn(resultSet);
    when(fileSystemHandler.newInputStream(any()))
        .thenReturn(new ByteArrayInputStream(
            "user0\n".getBytes(StandardCharsets.UTF_8.displayName())));
    doNothing().when(fileSystemHandler).copyOrReplace(any(Path.class), any());
    CompletableFuture<String> authFileContent = new CompletableFuture<>();
    doAnswer(invocation -> {
      authFileContent.complete(new String(
          InputStream.class.cast(invocation.getArgument(0))
          .readAllBytes(), StandardCharsets.UTF_8));
      return null;
    }).when(fileSystemHandler).copyOrReplace(any(InputStream.class), any());
    reconciliator.updatePgbouncerUsersInAuthFile(context);
    verify(fileSystemHandler, times(1)).copyOrReplace(any(Path.class), any());
    verify(fileSystemHandler, times(1)).copyOrReplace(any(InputStream.class), any());
    verify(postgresConnectionManager, times(1)).getConnection(any(), any(), any(), any(), any());
    verify(resultSet, times(1)).next();
    assertEquals("user0\n\n\n", authFileContent.join());
  }

  @Test
  void testReconciliationWithUsers_authFileIsUpdated() throws Exception {
    StackGresCluster cluster = JsonUtil
        .readFromJson("stackgres_cluster/list.json",
            StackGresClusterList.class)
        .getItems().get(0);
    StackGresPoolingConfig poolingConfig = JsonUtil
        .readFromJson("pooling_config/default.json",
            StackGresPoolingConfig.class);
    poolingConfig.getSpec().getPgBouncer().getPgbouncerIni().setUsers(
        ImmutableMap.of(
            "user1", ImmutableMap.of(),
            "user2", ImmutableMap.of()));
    Secret secret = JsonUtil
        .readFromJson("secret/patroni.json",
            Secret.class);
    when(context.getCluster()).thenReturn(cluster);
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(poolingConfig));
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(secret));
    when(postgresConnectionManager.getConnection(any(), any(), any(), any(), any()))
        .thenReturn(connection);
    when(connection.prepareStatement(any()))
        .thenReturn(preparedStatement);
    when(preparedStatement.executeQuery())
        .thenReturn(resultSet);
    when(resultSet.next())
        .thenReturn(true, true, false);
    when(resultSet.getString(eq(1)))
        .thenReturn("user1", "user2");
    when(fileSystemHandler.newInputStream(any()))
        .thenReturn(new ByteArrayInputStream(
            "user0\n".getBytes(StandardCharsets.UTF_8.displayName())));
    doNothing().when(fileSystemHandler).copyOrReplace(any(Path.class), any());
    CompletableFuture<String> authFileContent = new CompletableFuture<>();
    doAnswer(invocation -> {
      authFileContent.complete(new String(
          InputStream.class.cast(invocation.getArgument(0))
          .readAllBytes(), StandardCharsets.UTF_8));
      return null;
    }).when(fileSystemHandler).copyOrReplace(any(InputStream.class), any());
    reconciliator.updatePgbouncerUsersInAuthFile(context);
    verify(fileSystemHandler, times(1)).copyOrReplace(any(Path.class), any());
    verify(fileSystemHandler, times(1)).copyOrReplace(any(InputStream.class), any());
    verify(postgresConnectionManager, times(1)).getConnection(any(), any(), any(), any(), any());
    verify(resultSet, times(3)).next();
    verify(resultSet, times(2)).getString(eq(1));
    assertEquals("user0\n\nuser1\nuser2\n", authFileContent.join());
  }

}
