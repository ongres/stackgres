/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AbstractShardedClusterMetadataDecorator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedClusterMetadataDecorator
    extends AbstractShardedClusterMetadataDecorator<StackGresShardedClusterContext> {

  @Override
  protected Optional<StackGresShardedClusterSpecMetadata> getSpecMetadata(
      StackGresShardedClusterContext context) {
    return Optional.of(context.getShardedCluster())
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getMetadata);
  }

  @Override
  protected Optional<ObjectMeta> getMetadata(StackGresShardedClusterContext context) {
    return Optional.of(context.getShardedCluster()).map(StackGresShardedCluster::getMetadata);
  }

}
