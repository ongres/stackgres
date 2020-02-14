/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;

@ApplicationScoped
public class ClusterPipeline implements JsonPatchMutationPipeline<StackgresClusterReview> {

  private Instance<ClusterMutator> mutators;

  @Inject
  public ClusterPipeline(Instance<ClusterMutator> mutators) {
    this.mutators = mutators;
  }

  @Override
  public Optional<String> mutate(StackgresClusterReview review) {

    List<JsonPatchOperation> operations = mutators
        .stream()
        .map(m -> m.mutate(review))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    if (operations.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(join(operations));
    }
  }
}
