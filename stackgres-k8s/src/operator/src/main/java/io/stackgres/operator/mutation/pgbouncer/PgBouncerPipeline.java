/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;

@ApplicationScoped
public class PgBouncerPipeline implements JsonPatchMutationPipeline<PoolingReview> {

  private Instance<PgBouncerMutator> mutators;

  @Inject
  public PgBouncerPipeline(Instance<PgBouncerMutator> mutators) {
    this.mutators = mutators;
  }

  @Override
  public Optional<String> mutate(PoolingReview review) {
    List<JsonPatchOperation> operations = new ArrayList<>();

    mutators.forEach(mutator -> operations.addAll(mutator.mutate(review)));

    return operations.isEmpty()
        ? Optional.empty()
        : Optional.of(join(operations));
  }
}
