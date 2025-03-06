/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultCoordinatorPoolingMutator
    extends AbstractDefaultResourceMutator<
        StackGresPoolingConfig, HasMetadata, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  @Inject
  public DefaultCoordinatorPoolingMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig, HasMetadata> resourceFactory) {
    super(resourceFactory);
  }

  @Override
  protected void setValueSection(StackGresShardedCluster resource) {
    if (resource.getSpec().getCoordinator() == null) {
      resource.getSpec().setCoordinator(new StackGresShardedClusterCoordinator());
    }
    if (resource.getSpec().getCoordinator().getConfigurationsForCoordinator() == null) {
      resource.getSpec().getCoordinator().setConfigurationsForCoordinator(
          new StackGresShardedClusterCoordinatorConfigurations());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster resource) {
    return resource.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getSgPoolingConfig();
  }

  @Override
  protected void setTargetProperty(StackGresShardedCluster resource, String defaultResourceName) {
    resource.getSpec().getCoordinator().getConfigurationsForCoordinator().setSgPoolingConfig(
        defaultResourceName);
  }

}
