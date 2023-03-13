/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;

@ApplicationScoped
public class ShardedClusterPipeline
    implements JsonPatchMutationPipeline<StackGresShardedClusterReview> {

  private final Instance<ShardedClusterMutator> mutators;

  @Inject
  public ShardedClusterPipeline(Instance<ShardedClusterMutator> mutators) {
    this.mutators = mutators;
  }

  @Override
  public Optional<String> mutate(StackGresShardedClusterReview review) {
    return mutators.stream()
        .sorted(JsonPatchMutationPipeline.weightComparator())
        .map(m -> m.mutate(review))
        .flatMap(Collection::stream)
        .collect(Collectors.collectingAndThen(Collectors.toList(),
            JsonPatchMutationPipeline::join));
  }
}
