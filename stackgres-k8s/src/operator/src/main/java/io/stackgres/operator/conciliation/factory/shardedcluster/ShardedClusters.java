/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.function.Predicate;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class ShardedClusters implements ResourceGenerator<StackGresShardedClusterContext> {

  final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusters(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Seq.of(Boolean.TRUE)
        .filter(Predicate.not(ignore -> StackGresShardingType.SHARDING_SPHERE.equals(
            StackGresShardingType.fromString(context.getShardedCluster().getSpec().getType()))))
        .<HasMetadata>map(ignore -> context.getCoordinator())
        .map(coordinator -> {
          coordinator.getMetadata().setLabels(labelFactory.coordinatorLabels(context.getSource()));
          return coordinator;
        })
        .append(context.getShards().stream()
            .map(shards -> {
              shards.getMetadata().setLabels(labelFactory.shardsLabels(context.getSource()));
              return shards;
            }));
  }

}
