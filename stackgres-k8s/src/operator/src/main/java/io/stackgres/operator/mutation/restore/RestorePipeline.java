/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.restore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operatorframework.JsonPatchMutationPipeline;

@ApplicationScoped
public class RestorePipeline implements JsonPatchMutationPipeline<RestoreConfigReview> {

  private Instance<RestoreMutator> mutators;

  @Inject
  public RestorePipeline(Instance<RestoreMutator> mutators) {
    this.mutators = mutators;
  }

  @Override
  public Optional<String> mutate(RestoreConfigReview review) {
    List<JsonPatchOperation> operations = new ArrayList<>();

    mutators.forEach(mutator -> operations.addAll(mutator.mutate(review)));

    if (operations.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(join(operations));
    }
  }
}
