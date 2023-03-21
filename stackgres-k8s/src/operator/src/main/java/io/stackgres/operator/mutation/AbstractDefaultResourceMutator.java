/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.Collections;
import java.util.List;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public abstract class AbstractDefaultResourceMutator<C extends CustomResource<?, ?>,
        T extends CustomResource<?, ?>, R extends AdmissionReview<T>>
    implements JsonPatchMutator<R> {

  protected final DefaultCustomResourceFactory<C> resourceFactory;

  protected final CustomResourceFinder<C> finder;

  protected final CustomResourceScheduler<C> scheduler;

  private transient JsonPointer targetPointer;

  protected AbstractDefaultResourceMutator(
      DefaultCustomResourceFactory<C> resourceFactory,
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

  public void init() {
    this.targetPointer = getTargetPointer();
  }

  @Override
  public List<JsonPatchOperation> mutate(R review) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      C defaultResource = resourceFactory.buildResource();

      T target = review.getRequest().getObject();
      String targetNamespace = target.getMetadata().getNamespace();

      String defaultResourceName = defaultResource.getMetadata().getName();

      if (applyDefault(target)) {

        if (!finder.findByNameAndNamespace(defaultResourceName, targetNamespace).isPresent()) {
          defaultResource.getMetadata().setNamespace(targetNamespace);
          scheduler.create(defaultResource);
        }

        return Collections.singletonList(
            buildAddOperation(targetPointer, defaultResourceName));
      }

    }
    return List.of();
  }

  protected boolean applyDefault(T targetCluster) {
    return isTargetPropertyEmpty(targetCluster);
  }

  protected boolean isTargetPropertyEmpty(T targetCluster) {
    return isEmpty(getTargetPropertyValue(targetCluster));
  }

  protected abstract String getTargetPropertyValue(T targetCluster);

  protected abstract JsonPointer getTargetPointer();

}
