/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;

@ApplicationScoped
public class DbOpsPipeline implements JsonPatchMutationPipeline<DbOpsReview> {

  private Instance<DbOpsMutator> mutators;

  @Inject
  public DbOpsPipeline(Instance<DbOpsMutator> mutators) {
    this.mutators = mutators;
  }

  @Override
  public Optional<String> mutate(DbOpsReview review) {
    List<JsonPatchOperation> operations = new ArrayList<>();

    mutators.forEach(mutator -> operations.addAll(mutator.mutate(review)));

    if (operations.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(join(operations));
    }
  }
}
