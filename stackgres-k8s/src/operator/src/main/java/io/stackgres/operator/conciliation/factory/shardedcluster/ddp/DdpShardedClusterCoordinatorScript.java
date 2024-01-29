/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.ddp;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForDdpUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class DdpShardedClusterCoordinatorScript implements ResourceGenerator<StackGresShardedClusterContext> {

  final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public DdpShardedClusterCoordinatorScript(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Seq.of(Boolean.TRUE)
        .filter(ignore -> StackGresShardingType.DDP.equals(
            StackGresShardingType.fromString(context.getShardedCluster().getSpec().getType())))
        .<HasMetadata>map(ignore -> StackGresShardedClusterForDdpUtil
            .getCoordinatorScript(context))
        .map(config -> {
          config.getMetadata().setLabels(labelFactory.coordinatorLabels(context.getSource()));
          return config;
        });
  }

}
