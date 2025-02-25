/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public abstract class AbstractDefaultResourceMutator<C extends CustomResource<?, ?>,
        S extends HasMetadata, T extends S, R extends AdmissionReview<T>>
    implements Mutator<T, R> {

  protected final DefaultCustomResourceFactory<C, S> resourceFactory;

  protected final CustomResourceFinder<C> finder;

  protected final CustomResourceScheduler<C> scheduler;

  protected AbstractDefaultResourceMutator(
      DefaultCustomResourceFactory<C, S> resourceFactory,
      CustomResourceFinder<C> finder,
      CustomResourceScheduler<C> scheduler) {
    this.resourceFactory = resourceFactory;
    this.finder = finder;
    this.scheduler = scheduler;
  }

  public AbstractDefaultResourceMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.resourceFactory = null;
    this.finder = null;
    this.scheduler = null;
  }

  @Override
  public T mutate(R review, T resource) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return resource;
    }
    C defaultResource = resourceFactory.buildResource((S) resource);

    String defaultResourceName = defaultResource.getMetadata().getName();

    setValueSection(resource);
    if (isTargetPropertyEmpty(resource)) {
      setTargetProperty(resource, defaultResourceName);
    }

    return resource;
  }

  protected void setValueSection(T resource) {
  }

  private boolean isTargetPropertyEmpty(T resource) {
    return getTargetPropertyValue(resource) == null;
  }

  protected abstract String getTargetPropertyValue(T resource);

  protected abstract void setTargetProperty(T resource, String defaultResourceName);

}
