/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkerBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresWorkerType;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.tuple.Tuple3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterWorkersClustersContextAppenderTest {

  private ShardedClusterWorkersClustersContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private ShardedClusterWorkersInstanceProfileContextAppender
      shardedClusterWorkersInstanceProfileContextAppender;

  @Mock
  private ShardedClusterWorkersPostgresConfigContextAppender
      shardedClusterWorkersPostgresConfigContextAppender;

  @Mock
  private ShardedClusterWorkersPoolingConfigContextAppender
      shardedClusterWorkersPoolingConfigContextAppender;

  @Mock
  private ShardedClusterWorkersPrimaryEndpointsContextAppender
      shardedClusterWorkersPrimaryEndpointsContextAppender;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedClusterWorkersClustersContextAppender(
        shardedClusterWorkersInstanceProfileContextAppender,
        shardedClusterWorkersPostgresConfigContextAppender,
        shardedClusterWorkersPoolingConfigContextAppender,
        shardedClusterWorkersPrimaryEndpointsContextAppender,
        JsonUtil.jsonMapper());
  }

  @Test
  void givenCluster_shouldPass() {
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    contextAppender.appendContext(cluster, contextBuilder, postgresVersion, Optional.empty());
    ArgumentCaptor<List<StackGresCluster>> workers = ArgumentCaptor.captor();
    ArgumentCaptor<List<StackGresCluster>> queryRouters = ArgumentCaptor.captor();
    verify(contextBuilder).workers(workers.capture());
    verify(contextBuilder).queryRouters(queryRouters.capture());
    assertEquals(cluster.getSpec().getWorkers().getClusters(), workers.getValue().size());
    assertEquals(List.of(), queryRouters.getValue());
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedWorkers = ArgumentCaptor.captor();
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedQueryRouters = ArgumentCaptor.captor();
    verify(shardedClusterWorkersInstanceProfileContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), indexedWorkers.capture(), indexedQueryRouters.capture());
    assertEquals(
        workers.getValue(),
        indexedWorkers.getValue().stream().map(Tuple3::v3).toList());
    assertEquals(
        List.of(0, 1),
        indexedWorkers.getValue().stream().map(Tuple3::v1).toList());
    assertEquals(
        queryRouters.getValue(),
        indexedQueryRouters.getValue().stream().map(Tuple3::v3).toList());
    verify(shardedClusterWorkersPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, postgresVersion,
        indexedWorkers.getValue(), indexedQueryRouters.getValue());
    verify(shardedClusterWorkersPoolingConfigContextAppender).appendContext(
        cluster, contextBuilder,
        indexedWorkers.getValue(), indexedQueryRouters.getValue());
    verify(shardedClusterWorkersPrimaryEndpointsContextAppender).appendContext(
        workers.getValue(), queryRouters.getValue(), contextBuilder);
  }

  @Test
  void givenClusterWithQueryRouters_shouldPass() {
    cluster.getSpec().getCoordinator().setQueryRouterClusters(1);
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    contextAppender.appendContext(cluster, contextBuilder, postgresVersion, Optional.empty());
    ArgumentCaptor<List<StackGresCluster>> queryRouters = ArgumentCaptor.captor();
    verify(contextBuilder).queryRouters(queryRouters.capture());
    assertEquals(1, queryRouters.getValue().size());
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedWorkers = ArgumentCaptor.captor();
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedQueryRouters = ArgumentCaptor.captor();
    verify(shardedClusterWorkersInstanceProfileContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), indexedWorkers.capture(), indexedQueryRouters.capture());
    assertEquals(
        List.of(1024),
        indexedQueryRouters.getValue().stream().map(Tuple3::v1).toList());
    assertEquals(
        queryRouters.getValue(),
        indexedQueryRouters.getValue().stream().map(Tuple3::v3).toList());
  }

  @Test
  void givenClusterWithQueryRouterOverride_shouldOnlyApplyItToTheQueryRouter() {
    cluster.getSpec().getCoordinator().setQueryRouterClusters(1);
    cluster.getSpec().getWorkers().setOverrides(List.of(
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .withType(StackGresWorkerType.QUERY_ROUTER.toString())
        .withSgInstanceProfile("size-xs")
        .build()));
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    contextAppender.appendContext(cluster, contextBuilder, postgresVersion, Optional.empty());
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedWorkers = ArgumentCaptor.captor();
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedQueryRouters = ArgumentCaptor.captor();
    verify(shardedClusterWorkersInstanceProfileContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), indexedWorkers.capture(), indexedQueryRouters.capture());
    assertEquals(
        List.of(1024),
        indexedQueryRouters.getValue().stream().map(Tuple3::v1).toList());
    assertEquals(
        "size-xs",
        indexedQueryRouters.getValue().get(0).v2.orElseThrow().getSgInstanceProfile());
    assertTrue(
        indexedWorkers.getValue().stream().allMatch(worker -> worker.v2.isEmpty()),
        "a query router override must not be applied to the worker with the same index");
  }

  @Test
  void givenClusterWithWorkerOverride_shouldOnlyApplyItToTheWorker() {
    cluster.getSpec().getCoordinator().setQueryRouterClusters(1);
    cluster.getSpec().getWorkers().setOverrides(List.of(
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .withType(StackGresWorkerType.WORKER.toString())
        .withSgInstanceProfile("size-xs")
        .build()));
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    contextAppender.appendContext(cluster, contextBuilder, postgresVersion, Optional.empty());
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedWorkers = ArgumentCaptor.captor();
    ArgumentCaptor<List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>>>
        indexedQueryRouters = ArgumentCaptor.captor();
    verify(shardedClusterWorkersInstanceProfileContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), indexedWorkers.capture(), indexedQueryRouters.capture());
    assertEquals(
        "size-xs",
        indexedWorkers.getValue().get(0).v2.orElseThrow().getSgInstanceProfile());
    assertTrue(
        indexedWorkers.getValue().get(1).v2.isEmpty(),
        "a worker override must only be applied to the worker with the same index");
    assertTrue(
        indexedQueryRouters.getValue().stream().allMatch(queryRouter -> queryRouter.v2.isEmpty()),
        "a worker override must not be applied to a query router");
  }

}
