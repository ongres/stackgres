/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;

@ApplicationScoped
public class ShardedClusterWorkersClustersContextAppender {

  private final ShardedClusterWorkersInstanceProfileContextAppender
      shardedClusterWorkersInstanceProfileContextAppender;
  private final ShardedClusterWorkersPostgresConfigContextAppender
      shardedClusterWorkersPostgresConfigContextAppender;
  private final ShardedClusterWorkersPoolingConfigContextAppender
      shardedClusterWorkersPoolingConfigContextAppender;
  private final ShardedClusterWorkersPrimaryEndpointsContextAppender
      shardedClusterWorkersPrimaryEndpointsContextAppender;
  private final ObjectMapper objectMapper;

  public ShardedClusterWorkersClustersContextAppender(
      ShardedClusterWorkersInstanceProfileContextAppender
          shardedClusterWorkersInstanceProfileContextAppender,
      ShardedClusterWorkersPostgresConfigContextAppender
          shardedClusterWorkersPostgresConfigContextAppender,
      ShardedClusterWorkersPoolingConfigContextAppender
          shardedClusterWorkersPoolingConfigContextAppender,
      ShardedClusterWorkersPrimaryEndpointsContextAppender
          shardedClusterWorkersPrimaryEndpointsContextAppender,
      ObjectMapper objectMapper) {
    this.shardedClusterWorkersInstanceProfileContextAppender =
        shardedClusterWorkersInstanceProfileContextAppender;
    this.shardedClusterWorkersPostgresConfigContextAppender =
        shardedClusterWorkersPostgresConfigContextAppender;
    this.shardedClusterWorkersPoolingConfigContextAppender =
        shardedClusterWorkersPoolingConfigContextAppender;
    this.shardedClusterWorkersPrimaryEndpointsContextAppender =
        shardedClusterWorkersPrimaryEndpointsContextAppender;
    this.objectMapper = objectMapper;
  }

  public void appendContext(
      StackGresShardedCluster cluster,
      Builder contextBuilder,
      String postgresVersion,
      Optional<StackGresShardedCluster> replicateCluster) {
    var indexedWorkers = getWorkersClusters(
        cluster, cluster.getSpec().getWorkersOverrides(), replicateCluster);
    final List<StackGresCluster> workers = indexedWorkers.stream().map(Tuple3::v3).toList();
    contextBuilder.workers(workers);
    var indexedQueryRouters = getQueryRoutersClusters(
        cluster, cluster.getSpec().getQueryRoutersOverrides(), replicateCluster);
    final List<StackGresCluster> queryRouters = indexedQueryRouters.stream().map(Tuple3::v3).toList();
    contextBuilder.queryRouters(queryRouters);
    shardedClusterWorkersInstanceProfileContextAppender.appendContext(
        cluster, contextBuilder, indexedWorkers, indexedQueryRouters);
    shardedClusterWorkersPostgresConfigContextAppender.appendContext(
        cluster, contextBuilder, postgresVersion, indexedWorkers, indexedQueryRouters);
    shardedClusterWorkersPoolingConfigContextAppender.appendContext(
        cluster, contextBuilder, indexedWorkers, indexedQueryRouters);
    shardedClusterWorkersPrimaryEndpointsContextAppender.appendContext(workers, queryRouters, contextBuilder);
  }

  private List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> getWorkersClusters(
      StackGresShardedCluster cluster,
      List<StackGresShardedClusterWorker> workersOverrides,
      Optional<StackGresShardedCluster> replicateCluster) {
    return IntStream.range(0, cluster.getSpec().getWorkers().getClusters())
        .mapToObj(index -> Tuple.tuple(
            index,
            workersOverrides.stream()
            .filter(override -> Objects.equals(override.getIndex(), index))
            .findFirst(),
            getWorkerCluster(cluster, index, replicateCluster)))
        .toList();
  }

  private StackGresCluster getWorkerCluster(
      StackGresShardedCluster original,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    StackGresShardedCluster cluster = objectMapper.convertValue(original, StackGresShardedCluster.class);
    return StackGresShardedClusterForUtil.getWorkerCluster(cluster, index, replicateCluster);
  }

  private List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> getQueryRoutersClusters(
      StackGresShardedCluster cluster,
      List<StackGresShardedClusterWorker> queryRoutersOverrides,
      Optional<StackGresShardedCluster> replicateCluster) {
    final int queryRouterIndexOffset =
        Optional.ofNullable(cluster.getSpec().getCoordinator().getQueryRouterIndexOffset())
        .orElse(1024);
    final int queryRouterClusters = Optional.ofNullable(cluster.getSpec().getCoordinator().getQueryRouterClusters())
        .orElse(0);
    return IntStream.range(
            queryRouterIndexOffset,
            queryRouterIndexOffset + queryRouterClusters)
        .mapToObj(index -> Tuple.tuple(
            index,
            queryRoutersOverrides.stream()
            .filter(override -> Objects.equals(override.getIndex(), index))
            .findFirst(),
            getQueryRouterCluster(cluster, index, replicateCluster)))
        .toList();
  }

  private StackGresCluster getQueryRouterCluster(
      StackGresShardedCluster original,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    StackGresShardedCluster cluster = objectMapper.convertValue(original, StackGresShardedCluster.class);
    return StackGresShardedClusterForUtil.getQueryRouterCluster(cluster, index, replicateCluster);
  }

}
