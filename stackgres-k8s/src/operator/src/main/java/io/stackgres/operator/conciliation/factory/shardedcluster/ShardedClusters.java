/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
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
    return Seq.<HasMetadata>of(context.getCoordinator().getCluster())
        .map(coordinator -> {
          coordinator.getMetadata().setLabels(labelFactory.coordinatorLabels(context.getSource()));
          return coordinator;
        })
        .append(context.getShards().stream().map(StackGresClusterContext::getCluster)
            .map(shards -> {
              shards.getMetadata().setLabels(labelFactory.shardsLabels(context.getSource()));
              return shards;
            }));
  }

}
