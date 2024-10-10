/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterWorkersPostgresConfigContextAppenderTest {

  private ShardedClusterWorkersPostgresConfigContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  private List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> workers;

  private List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> queryRouters;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  private DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory =
      new DefaultShardedClusterPostgresConfigFactory(StackGresContextMock.CONTEXT);

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    workers = List.of(
        Tuple.tuple(0, Optional.empty(), StackGresShardedClusterForCitusUtil
            .getWorkerCluster(cluster, 0, Optional.empty())),
        Tuple.tuple(1, Optional.empty(), StackGresShardedClusterForCitusUtil
            .getWorkerCluster(cluster, 1, Optional.empty())));
    queryRouters = List.of(
        Tuple.tuple(1024, Optional.empty(), StackGresShardedClusterForCitusUtil
            .getQueryRouterCluster(cluster, 1024, Optional.empty())));
    contextAppender = new ShardedClusterWorkersPostgresConfigContextAppender(
        StackGresContextMock.CONTEXT,
        postgresConfigFinder,
        defaultPostgresConfigFactory);
  }

  @Test
  void givenClusterWithPostgresConfig_shouldPass() {
    final var postgresConfig = Optional.of(
        new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion().replaceAll("\\..*$", ""))
        .endSpec()
        .build());
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(postgresConfig);
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion(),
        workers, queryRouters);
    verify(contextBuilder).workersPostgresConfigs(List.of(
        Tuple.tuple(0, postgresConfig),
        Tuple.tuple(1, postgresConfig)));
    verify(contextBuilder).queryRoutersPostgresConfigs(List.of(
        Tuple.tuple(1024, postgresConfig)));
  }

  @Test
  void givenClusterWithoutPostgresConfig_shouldFail() {
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion(),
            workers, queryRouters));
    assertEquals("SGPostgresConfig postgresconf was not found", ex.getMessage());
  }

  @Test
  void givenClusterWithPostgresConfigWithWrongVersion_shouldFail() {
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(
            new StackGresPostgresConfigBuilder()
            .withNewSpec()
            .withPostgresVersion("10")
            .endSpec()
            .build()));
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion(),
            workers, queryRouters));
    assertEquals("Invalid postgres version, must be 10 to use SGPostgresConfig postgresconf", ex.getMessage());
  }

  @Test
  void givenClusterWithoutDefaultPostgresConfig_shouldPass() {
    cluster.getSpec().getWorkers().getConfigurations().setSgPostgresConfig(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion(),
        workers, queryRouters);
    verify(contextBuilder).workersPostgresConfigs(List.of(
        Tuple.tuple(0, Optional.empty()),
        Tuple.tuple(1, Optional.empty())));
    verify(contextBuilder).queryRoutersPostgresConfigs(List.of(
        Tuple.tuple(1024, Optional.empty())));
  }

}
