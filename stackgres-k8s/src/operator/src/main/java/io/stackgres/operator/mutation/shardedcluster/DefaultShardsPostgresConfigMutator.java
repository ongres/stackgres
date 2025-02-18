/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultShardsPostgresConfigMutator
    extends AbstractDefaultResourceMutator<
        StackGresPostgresConfig, StackGresShardedCluster, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  public DefaultShardsPostgresConfigMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresShardedCluster> resourceFactory) {
    super(resourceFactory);
  }

  @Override
  protected void setValueSection(StackGresShardedCluster resource) {
    if (resource.getSpec().getShards() == null) {
      resource.getSpec().setShards(
          new StackGresShardedClusterShards());
    }
    if (resource.getSpec().getShards().getConfigurations() == null) {
      resource.getSpec().getShards().setConfigurations(
          new StackGresClusterConfigurations());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster resource) {
    return resource.getSpec().getShards().getConfigurations().getSgPostgresConfig();
  }

  @Override
  protected void setTargetProperty(StackGresShardedCluster resource, String defaultResourceName) {
    resource.getSpec().getShards().getConfigurations().setSgPostgresConfig(defaultResourceName);
  }

}
