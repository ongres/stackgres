/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.RemoveOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DbOpsStatusResetMutator
    implements DbOpsMutator {

  static final JsonPointer STATUS_POINTER = JsonPointer.of("status");

  @Override
  public List<JsonPatchOperation> mutate(DbOpsReview review) {
    AdmissionRequest<StackGresDbOps> request = review.getRequest();
    if (request.getOperation() == Operation.CREATE
        && request.getObject().getStatus() != null) {
      return ImmutableList.of(new RemoveOperation(STATUS_POINTER));
    } else {
      return ImmutableList.of();
    }
  }

}
