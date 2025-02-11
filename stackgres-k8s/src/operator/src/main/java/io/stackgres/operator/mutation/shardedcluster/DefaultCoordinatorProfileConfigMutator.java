/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
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
public class DefaultCoordinatorProfileConfigMutator
    extends AbstractDefaultResourceMutator<
        StackGresProfile, HasMetadata, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  @Inject
  public DefaultCoordinatorProfileConfigMutator(
      DefaultCustomResourceFactory<StackGresProfile, HasMetadata> resourceFactory,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  protected void setValueSection(StackGresShardedCluster resource) {
    if (resource.getSpec().getCoordinator() == null) {
      resource.getSpec().setCoordinator(
          new StackGresShardedClusterCoordinator());
    }
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster resource) {
    return resource.getSpec().getCoordinator().getSgInstanceProfile();
  }

  @Override
  protected void setTargetProperty(StackGresShardedCluster resource, String defaultResourceName) {
    resource.getSpec().getCoordinator().setSgInstanceProfile(defaultResourceName);
  }

}
