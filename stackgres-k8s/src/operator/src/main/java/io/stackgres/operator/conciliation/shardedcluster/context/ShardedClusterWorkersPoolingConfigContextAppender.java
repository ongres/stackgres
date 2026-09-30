/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

@ApplicationScoped
public class ShardedClusterWorkersPoolingConfigContextAppender {

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;
  private final DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  public ShardedClusterWorkersPoolingConfigContextAppender(
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      DefaultPoolingConfigFactory defaultPoolingConfigFactory) {
    this.poolingConfigFinder = poolingConfigFinder;
    this.defaultPoolingConfigFactory = defaultPoolingConfigFactory;
  }

  public void appendContext(
      StackGresShardedCluster cluster,
      Builder contextBuilder,
      List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> workers,
      List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> queryRouters) {
    var workersPoolingConfigs = workers
        .stream()
        .map(worker -> findPoolingConfig(cluster, worker,
            cluster.getSpec().getWorkers().getConfigurations().getSgPoolingConfig(),
            cluster.getSpec().getWorkers().getPods().getDisableConnectionPooling()))
        .toList();
    contextBuilder.workersPoolingConfigs(workersPoolingConfigs);
    // Query routers inherit their spec from the coordinator (see
    // StackGresShardedClusterForUtil.getBaseQueryRouterCluster), so the coordinator
    // SGPoolingConfig and pods configuration, and not the workers one, are their default.
    var queryRoutersPoolingConfigs = queryRouters
        .stream()
        .map(queryRouter -> findPoolingConfig(cluster, queryRouter,
            cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
            .getSgPoolingConfig(),
            cluster.getSpec().getCoordinator().getPods().getDisableConnectionPooling()))
        .toList();
    contextBuilder.queryRoutersPoolingConfigs(queryRoutersPoolingConfigs);
  }

  private Tuple2<Integer, Optional<StackGresPoolingConfig>> findPoolingConfig(
      StackGresShardedCluster cluster,
      Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster> worker,
      String defaultPoolingConfigName,
      Boolean defaultDisableConnectionPooling) {
    // The overrides carry their configurations and pods in configurationsForWorkers and
    // podsForWorkers (JSON `configurations` and `pods`), the inherited StackGresClusterSpec
    // fields are never set on them.
    final String workerPoolingConfigName = worker.v2
        .map(StackGresShardedClusterWorker::getConfigurationsForWorkers)
        .map(StackGresClusterConfigurations::getSgPoolingConfig)
        .orElse(defaultPoolingConfigName);
    final Boolean workerDisableConnectionPooling = worker.v2
        .map(StackGresShardedClusterWorker::getPodsForWorkers)
        .map(StackGresClusterPods::getDisableConnectionPooling)
        .orElse(defaultDisableConnectionPooling);
    final Optional<StackGresPoolingConfig> workersPoolingConfig = Optional
        .ofNullable(workerPoolingConfigName)
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(
                poolingConfigName,
                cluster.getMetadata().getNamespace()));
    if (workerPoolingConfigName != null
        && !workerPoolingConfigName
        .equals(defaultPoolingConfigFactory.getDefaultResourceName(cluster))
        && !Optional.ofNullable(workerDisableConnectionPooling).orElse(false)
        && workersPoolingConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPoolingConfig.KIND + " "
              + workerPoolingConfigName
              + " was not found");
    }
    return Tuple.tuple(worker.v1, workersPoolingConfig);
  }

}
