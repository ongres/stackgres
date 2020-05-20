/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;

import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public abstract class AbstractAnnotationMutator
    <R extends CustomResource, T extends AdmissionReview<R>>
    implements DefaultAnnotationMutator<R, T> {

  private ConfigContext configContext;

  @Override
  public List<JsonPatchOperation> mutate(T review) {
    AdmissionRequest<R> request = review.getRequest();
    if (request.getOperation() == Operation.CREATE) {
      return getAnnotationsToAdd(request.getObject(), configContext);
    } else {
      return ImmutableList.of();
    }
  }

  @Inject
  public void setConfigContext(ConfigContext configContext) {
    this.configContext = configContext;
  }
}
