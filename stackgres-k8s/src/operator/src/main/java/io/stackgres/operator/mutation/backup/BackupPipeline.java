/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;

@ApplicationScoped
public class BackupPipeline implements JsonPatchMutationPipeline<BackupReview> {

  private Instance<BackupMutator> mutators;

  @Inject
  public BackupPipeline(Instance<BackupMutator> mutators) {
    this.mutators = mutators;
  }

  @Override
  public Optional<String> mutate(BackupReview review) {
    List<JsonPatchOperation> operations = new ArrayList<>();

    mutators.forEach(mutator -> operations.addAll(mutator.mutate(review)));

    if (operations.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(join(operations));
    }
  }
}
