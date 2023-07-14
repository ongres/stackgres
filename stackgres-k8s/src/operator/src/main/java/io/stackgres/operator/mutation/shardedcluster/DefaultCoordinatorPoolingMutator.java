/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultCoordinatorPoolingMutator
    extends AbstractDefaultResourceMutator<
        StackGresPoolingConfig, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  @Inject
  public DefaultCoordinatorPoolingMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig> resourceFactory,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  protected void setValueSection(StackGresShardedCluster resource) {
    if (resource.getSpec().getCoordinator() == null) {
      resource.getSpec().setCoordinator(new StackGresShardedClusterCoordinator());
    }
    if (resource.getSpec().getCoordinator().getConfiguration() == null) {
      resource.getSpec().getCoordinator().setConfiguration(new StackGresClusterConfiguration());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster resource) {
    return resource.getSpec().getCoordinator().getConfiguration().getConnectionPoolingConfig();
  }

  @Override
  protected void setTargetProperty(StackGresShardedCluster resource, String defaultResourceName) {
    resource.getSpec().getCoordinator().getConfiguration().setConnectionPoolingConfig(
        defaultResourceName);
  }

}
