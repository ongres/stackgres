/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultCoordinatorPostgresConfigMutator
    extends AbstractDefaultResourceMutator<
        StackGresPostgresConfig, StackGresShardedCluster, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  public DefaultCoordinatorPostgresConfigMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresShardedCluster> resourceFactory) {
    super(resourceFactory);
  }

  @Override
  protected void setValueSection(StackGresShardedCluster resource) {
    if (resource.getSpec().getCoordinator() == null) {
      resource.getSpec().setCoordinator(
          new StackGresShardedClusterCoordinator());
    }
    if (resource.getSpec().getCoordinator().getConfigurationsForCoordinator() == null) {
      resource.getSpec().getCoordinator().setConfigurationsForCoordinator(
          new StackGresShardedClusterCoordinatorConfigurations());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster resource) {
    return resource.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getSgPostgresConfig();
  }

  @Override
  protected void setTargetProperty(StackGresShardedCluster resource, String defaultResourceName) {
    resource.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .setSgPostgresConfig(defaultResourceName);
  }

}
