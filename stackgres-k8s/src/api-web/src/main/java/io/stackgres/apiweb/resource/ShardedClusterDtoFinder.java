/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.Optional;

import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.transformer.ShardedClusterTransformer;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterDtoFinder implements CustomResourceFinder<ShardedClusterDto> {

  private CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;
  private ShardedClusterTransformer shardedClusterTransformer;

  @Override
  public Optional<ShardedClusterDto> findByNameAndNamespace(String name, String namespace) {
    return shardedClusterFinder.findByNameAndNamespace(name, namespace)
        .map(resrouce -> shardedClusterTransformer.toDto(resrouce));
  }

  @Inject
  public void setShardedClusterFinder(
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder) {
    this.shardedClusterFinder = shardedClusterFinder;
  }

  @Inject
  public void setShardedClusterTransformer(ShardedClusterTransformer shardedClusterTransformer) {
    this.shardedClusterTransformer = shardedClusterTransformer;
  }

}
