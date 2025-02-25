/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterCoordinatorPostgresConfigContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory;

  public ShardedClusterCoordinatorPostgresConfigContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory) {
    this.postgresConfigFinder = postgresConfigFinder;
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final Optional<StackGresPostgresConfig> coordinatorPostgresConfig = postgresConfigFinder
        .findByNameAndNamespace(
            cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig(),
            cluster.getMetadata().getNamespace());
    if (!cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig().equals(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster))
        && coordinatorPostgresConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND + " "
          + cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig()
          + " was not found");
    }
    String givenPgVersion = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(null);
    String clusterMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(givenPgVersion);
    if (coordinatorPostgresConfig.isPresent()) {
      String postgresConfigVersion = coordinatorPostgresConfig.get().getSpec().getPostgresVersion();
      if (!postgresConfigVersion.equals(clusterMajorVersion)) {
        throw new IllegalArgumentException(
            "Invalid postgres version, must be "
                + postgresConfigVersion + " to use SGPostgresConfig "
                + cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig());
      }
    }
    contextBuilder.coordinatorPostgresConfig(coordinatorPostgresConfig);
  }

}
