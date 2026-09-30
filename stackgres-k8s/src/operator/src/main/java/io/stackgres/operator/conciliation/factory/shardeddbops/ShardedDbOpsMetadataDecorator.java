/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AbstractShardedClusterMetadataDecorator;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedDbOpsMetadataDecorator
    extends AbstractShardedClusterMetadataDecorator<StackGresShardedDbOpsContext> {

  @Override
  protected Optional<StackGresShardedClusterSpecMetadata> getSpecMetadata(
      StackGresShardedDbOpsContext context) {
    return context.getFoundShardedCluster()
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getMetadata);
  }

  @Override
  protected Optional<ObjectMeta> getMetadata(StackGresShardedDbOpsContext context) {
    return Optional.of(context.getSource()).map(StackGresShardedDbOps::getMetadata);
  }

}
