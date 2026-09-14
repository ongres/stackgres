/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkerBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresWorkerType;
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
      new DefaultShardedClusterPostgresConfigFactory();

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
    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().setSgPostgresConfig(
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

  @Test
  void givenClusterWithoutQueryRouterOverride_shouldUseCoordinatorPostgresConfig() {
    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .setSgPostgresConfig("coordinator-postgresconf");
    final var workersPostgresConfig = Optional.of(postgresConfig("postgresconf"));
    final var coordinatorPostgresConfig = Optional.of(postgresConfig("coordinator-postgresconf"));
    when(postgresConfigFinder.findByNameAndNamespace(eq("postgresconf"), any()))
        .thenReturn(workersPostgresConfig);
    when(postgresConfigFinder.findByNameAndNamespace(eq("coordinator-postgresconf"), any()))
        .thenReturn(coordinatorPostgresConfig);
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion(),
        workers, queryRouters);
    verify(contextBuilder).workersPostgresConfigs(List.of(
        Tuple.tuple(0, workersPostgresConfig),
        Tuple.tuple(1, workersPostgresConfig)));
    verify(contextBuilder).queryRoutersPostgresConfigs(List.of(
        Tuple.tuple(1024, coordinatorPostgresConfig)));
  }

  @Test
  void givenClusterWithQueryRouterOverride_shouldUseOverriddenPostgresConfig() {
    queryRouters = List.of(
        Tuple.tuple(1024, Optional.of(
            new StackGresShardedClusterWorkerBuilder()
            .withIndex(1024)
            .withType(StackGresWorkerType.QUERY_ROUTER.toString())
            .withNewConfigurationsForWorkers()
            .withSgPostgresConfig("router-postgresconf")
            .endConfigurationsForWorkers()
            .build()),
            StackGresShardedClusterForCitusUtil
            .getQueryRouterCluster(cluster, 1024, Optional.empty())));
    final var workersPostgresConfig = Optional.of(postgresConfig("postgresconf"));
    final var overriddenPostgresConfig = Optional.of(postgresConfig("router-postgresconf"));
    when(postgresConfigFinder.findByNameAndNamespace(eq("postgresconf"), any()))
        .thenReturn(workersPostgresConfig);
    when(postgresConfigFinder.findByNameAndNamespace(eq("router-postgresconf"), any()))
        .thenReturn(overriddenPostgresConfig);
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion(),
        workers, queryRouters);
    verify(contextBuilder).workersPostgresConfigs(List.of(
        Tuple.tuple(0, workersPostgresConfig),
        Tuple.tuple(1, workersPostgresConfig)));
    verify(contextBuilder).queryRoutersPostgresConfigs(List.of(
        Tuple.tuple(1024, overriddenPostgresConfig)));
  }

  @Test
  void givenClusterWithWorkerOverride_shouldUseOverriddenPostgresConfig() {
    workers = List.of(
        Tuple.tuple(0, Optional.of(
            new StackGresShardedClusterWorkerBuilder()
            .withIndex(0)
            .withNewConfigurationsForWorkers()
            .withSgPostgresConfig("worker-postgresconf")
            .endConfigurationsForWorkers()
            .build()),
            StackGresShardedClusterForCitusUtil
            .getWorkerCluster(cluster, 0, Optional.empty())),
        workers.get(1));
    final var workersPostgresConfig = Optional.of(postgresConfig("postgresconf"));
    final var overriddenPostgresConfig = Optional.of(postgresConfig("worker-postgresconf"));
    when(postgresConfigFinder.findByNameAndNamespace(eq("postgresconf"), any()))
        .thenReturn(workersPostgresConfig);
    when(postgresConfigFinder.findByNameAndNamespace(eq("worker-postgresconf"), any()))
        .thenReturn(overriddenPostgresConfig);
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion(),
        workers, queryRouters);
    verify(contextBuilder).workersPostgresConfigs(List.of(
        Tuple.tuple(0, overriddenPostgresConfig),
        Tuple.tuple(1, workersPostgresConfig)));
    verify(contextBuilder).queryRoutersPostgresConfigs(List.of(
        Tuple.tuple(1024, workersPostgresConfig)));
  }

  private StackGresPostgresConfig postgresConfig(String name) {
    return new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .withNewSpec()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion().replaceAll("\\..*$", ""))
        .endSpec()
        .build();
  }

}
