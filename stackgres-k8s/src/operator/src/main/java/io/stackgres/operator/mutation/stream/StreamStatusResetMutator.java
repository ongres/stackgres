/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.stream;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StreamStatusResetMutator implements StreamMutator {

  @Override
  public StackGresStream mutate(StackGresStreamReview review, StackGresStream resource) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      resource.setStatus(null);
    }
    return resource;
  }

}
