/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;

@ApplicationScoped
public class PgConfigPipeline implements JsonPatchMutationPipeline<PgConfigReview> {

  private Instance<PgConfigMutator> mutators;

  @Inject
  public PgConfigPipeline(Instance<PgConfigMutator> mutators) {
    this.mutators = mutators;
  }

  @Override
  public Optional<String> mutate(PgConfigReview review) {
    List<JsonPatchOperation> operations = new ArrayList<>();

    mutators.forEach(pgConfigMutator -> operations.addAll(pgConfigMutator.mutate(review)));

    return operations.isEmpty()
        ? Optional.empty()
        : Optional.of(join(operations));
  }
}
