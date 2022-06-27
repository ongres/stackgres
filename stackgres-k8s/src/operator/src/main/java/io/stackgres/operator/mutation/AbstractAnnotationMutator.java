/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public abstract class AbstractAnnotationMutator
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements DefaultAnnotationMutator<R, T> {

  @Override
  public List<JsonPatchOperation> mutate(T review) {
    AdmissionRequest<R> request = review.getRequest();
    if (request.getOperation() == Operation.CREATE) {
      return getAnnotationsToAdd(request.getObject());
    } else {
      return List.of();
    }
  }

}
