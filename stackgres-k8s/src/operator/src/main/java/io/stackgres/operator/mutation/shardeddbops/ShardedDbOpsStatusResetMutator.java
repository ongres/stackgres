/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedDbOpsStatusResetMutator implements ShardedDbOpsMutator {

  @Override
  public StackGresShardedDbOps mutate(ShardedDbOpsReview review, StackGresShardedDbOps resource) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      resource.setStatus(null);
    }
    return resource;
  }

}
