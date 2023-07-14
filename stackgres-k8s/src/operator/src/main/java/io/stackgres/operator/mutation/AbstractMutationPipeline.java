/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.CdiUtil;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;
import jakarta.enterprise.inject.Instance;

public abstract class AbstractMutationPipeline<R extends HasMetadata, T extends AdmissionReview<R>>
    extends MutationPipeline<R, T> {

  protected AbstractMutationPipeline(Instance<? extends Mutator<R, T>> mutators) {
    super(mutators.stream().toList());
  }

  public AbstractMutationPipeline() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }
}
