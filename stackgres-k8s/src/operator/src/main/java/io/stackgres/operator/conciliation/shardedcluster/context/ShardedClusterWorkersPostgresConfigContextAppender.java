/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

@ApplicationScoped
public class ShardedClusterWorkersPostgresConfigContextAppender {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory;

  public ShardedClusterWorkersPostgresConfigContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory) {
    this.postgresConfigFinder = postgresConfigFinder;
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
  }

  public void appendContext(
      StackGresShardedCluster cluster,
      Builder contextBuilder,
      String postgresVersion,
      List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> workers,
      List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> queryRouters) {
    var workersPostgresConfigs = workers
        .stream()
        .map(worker -> findPostgresConfig(cluster, postgresVersion, worker,
            cluster.getSpec().getWorkers().getConfigurations().getSgPostgresConfig()))
        .toList();
    contextBuilder.workersPostgresConfigs(workersPostgresConfigs);
    // Query routers inherit their spec from the coordinator (see
    // StackGresShardedClusterForUtil.getBaseQueryRouterCluster), so the coordinator
    // SGPostgresConfig, and not the workers one, is their default.
    var queryRoutersPostgresConfigs = queryRouters
        .stream()
        .map(queryRouter -> findPostgresConfig(cluster, postgresVersion, queryRouter,
            cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
            .getSgPostgresConfig()))
        .toList();
    contextBuilder.queryRoutersPostgresConfigs(queryRoutersPostgresConfigs);
  }

  private Tuple2<Integer, Optional<StackGresPostgresConfig>> findPostgresConfig(
      StackGresShardedCluster cluster,
      String postgresVersion,
      Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster> worker,
      String defaultPostgresConfigName) {
    // The overrides carry their configurations in configurationsForWorkers (JSON `configurations`),
    // the inherited StackGresClusterSpec.configurations field is never set on them.
    final String workerPostgresConfigName = worker.v2
        .map(StackGresShardedClusterWorker::getConfigurationsForWorkers)
        .map(StackGresClusterConfigurations::getSgPostgresConfig)
        .orElse(defaultPostgresConfigName);
    final Optional<StackGresPostgresConfig> workersPostgresConfig = postgresConfigFinder
        .findByNameAndNamespace(
            workerPostgresConfigName,
            cluster.getMetadata().getNamespace());
    if (!workerPostgresConfigName.equals(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster))
        && workersPostgresConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND + " "
          + workerPostgresConfigName
          + " was not found");
    }
    String postgresMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(postgresVersion);
    if (workersPostgresConfig.isPresent()) {
      String postgresConfigVersion = workersPostgresConfig.get().getSpec().getPostgresVersion();
      if (!postgresConfigVersion.equals(postgresMajorVersion)) {
        throw new IllegalArgumentException(
            "Invalid postgres version, must be "
                + postgresConfigVersion + " to use SGPostgresConfig "
                + workerPostgresConfigName);
      }
    }
    return Tuple.tuple(worker.v1, workersPostgresConfig);
  }

}
