/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public abstract class AbstractStateMutator
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements Mutator<R, T> {

  private final DefaultCustomResourceFactory<R> factory;

  private R defaultResource;

  public AbstractStateMutator(DefaultCustomResourceFactory<R> factory) {
    this.factory = factory;
  }

  public void init() {
    this.defaultResource = factory.buildResource();
  }

  @Override
  public R mutate(T review, R resource) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return resource;
    }
    setDefaultState(resource, defaultResource);
    return resource;
  }

  protected abstract void setDefaultState(R resource, R defaultResource);

}
