/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.util.List;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;

public abstract class MutationPipeline<R extends HasMetadata, T extends AdmissionReview<R>> {

  private final List<? extends Mutator<R, T>> mutators;

  protected MutationPipeline(List<? extends Mutator<R, T>> mutators) {
    this.mutators = mutators;
  }

  @Nonnull
  public R mutate(@Nonnull T review, R resource) {
    for (var mutator : mutators) {
      resource = mutator.mutate(review, resource);
    }
    return resource;
  }

}
