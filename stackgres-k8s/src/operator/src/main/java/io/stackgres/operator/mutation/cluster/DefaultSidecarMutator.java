/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operatorframework.Operation;

@ApplicationScoped
public class DefaultSidecarMutator implements ClusterMutator {

  private JsonPointer sidecarsPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    sidecarsPointer = getTargetPointer("sidecars");
  }

  @Override
  public List<JsonPatchOperation> mutate(StackgresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        && review.getRequest().getObject().getSpec().getSidecars() == null) {
      return ImmutableList.of(new AddOperation(sidecarsPointer, FACTORY.arrayNode()));
    }
    return ImmutableList.of();
  }

}
