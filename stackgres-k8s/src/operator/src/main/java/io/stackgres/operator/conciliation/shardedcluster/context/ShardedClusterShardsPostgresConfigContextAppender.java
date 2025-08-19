/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Optional;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterShardsPostgresConfigContextAppender {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory;

  public ShardedClusterShardsPostgresConfigContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory) {
    this.postgresConfigFinder = postgresConfigFinder;
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
  }

  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder, String postgresVersion) {
    final Optional<StackGresPostgresConfig> shardsPostgresConfig = postgresConfigFinder
        .findByNameAndNamespace(
            cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
            cluster.getMetadata().getNamespace());
    if (!cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig().equals(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster))
        && shardsPostgresConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND + " "
          + cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig()
          + " was not found");
    }
    String postgresMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(postgresVersion);
    if (shardsPostgresConfig.isPresent()) {
      String postgresConfigVersion = shardsPostgresConfig.get().getSpec().getPostgresVersion();
      if (!postgresConfigVersion.equals(postgresMajorVersion)) {
        throw new IllegalArgumentException(
            "Invalid postgres version, must be "
                + postgresConfigVersion + " to use SGPostgresConfig "
                + cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig());
      }
    }
    contextBuilder.shardsPostgresConfig(shardsPostgresConfig);
  }

}
