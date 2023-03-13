/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.List;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public class DefaultCoordinatorPostgresMutator
    extends AbstractDefaultResourceMutator<
        StackGresPostgresConfig, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterCoordinatorConfigurationMutator {

  public DefaultCoordinatorPostgresMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig> resourceFactory,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresShardedClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      operations.addAll(ensureConfigurationNode(review));
      operations.addAll(super.mutate(review));
      return operations.build();

    }
    return ImmutableList.of();
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster targetCluster) {
    return targetCluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig();
  }

  @Override
  public JsonPointer getTargetPointer() {
    return getConfigurationTargetPointer("postgresConfig");
  }
}
