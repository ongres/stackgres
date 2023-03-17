/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DbOpsStatusResetMutator implements DbOpsMutator {

  @Override
  public StackGresDbOps mutate(DbOpsReview review, StackGresDbOps resource) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      resource.setStatus(null);
    }
    return resource;
  }

}
