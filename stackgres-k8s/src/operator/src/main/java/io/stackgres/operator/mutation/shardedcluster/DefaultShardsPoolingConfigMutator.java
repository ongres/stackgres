/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultShardsPoolingConfigMutator
    extends AbstractDefaultResourceMutator<
        StackGresPoolingConfig, HasMetadata, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  @Inject
  public DefaultShardsPoolingConfigMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig, HasMetadata> resourceFactory,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  protected void setValueSection(StackGresShardedCluster resource) {
    if (resource.getSpec().getShards() == null) {
      resource.getSpec().setShards(new StackGresShardedClusterShards());
    }
    if (resource.getSpec().getShards().getConfigurations() == null) {
      resource.getSpec().getShards().setConfigurations(new StackGresClusterConfigurations());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster resource) {
    return resource.getSpec().getShards().getConfigurations().getSgPoolingConfig();
  }

  @Override
  protected void setTargetProperty(StackGresShardedCluster resource, String defaultResourceName) {
    resource.getSpec().getShards().getConfigurations().setSgPoolingConfig(
        defaultResourceName);
  }

}
