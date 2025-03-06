/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.FireAndForgetReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresShardedCluster.class, kind = StackGresPoolingConfig.KIND)
@ApplicationScoped
public class ShardedClusterPoolingConfigReconciliationHandler
    extends FireAndForgetReconciliationHandler<StackGresShardedCluster> {

  private final LabelFactoryForShardedCluster labelFactory;

  public ShardedClusterPoolingConfigReconciliationHandler(
      @ReconciliationScope(value = StackGresShardedCluster.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresShardedCluster> handler,
      LabelFactoryForShardedCluster labelFactory) {
    super(handler);
    this.labelFactory = labelFactory;
  }

  @Override
  protected boolean canForget(StackGresShardedCluster context, HasMetadata resource) {
    return labelFactory.defaultConfigLabels(context)
        .entrySet()
        .stream()
        .allMatch(label -> Optional.of(resource.getMetadata().getLabels())
            .stream()
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .anyMatch(label::equals));
  }

}
