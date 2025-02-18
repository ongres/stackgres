/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterShardsInstanceProfileContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final CustomResourceFinder<StackGresProfile> profileFinder;
  private final DefaultProfileFactory defaultProfileFactory;

  public ShardedClusterShardsInstanceProfileContextAppender(
      CustomResourceFinder<StackGresProfile> profileFinder,
      DefaultProfileFactory defaultProfileFactory) {
    this.profileFinder = profileFinder;
    this.defaultProfileFactory = defaultProfileFactory;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final Optional<StackGresProfile> shardsProfile = profileFinder
        .findByNameAndNamespace(
            cluster.getSpec().getShards().getSgInstanceProfile(),
            cluster.getMetadata().getNamespace());
    if (!cluster.getSpec().getShards().getSgInstanceProfile()
        .equals(defaultProfileFactory.getDefaultResourceName(cluster))
        && shardsProfile.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresProfile.KIND + " " + cluster.getSpec().getShards().getSgInstanceProfile() + " was not found");
    }
    contextBuilder.shardsProfile(shardsProfile);
  }

}
