/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;

import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class PgBouncerDefaultValuesMutator
    extends DefaultValuesMutator<StackGresPgbouncerConfig, PgBouncerReview>
    implements PgBouncerMutator {

  @Override
  public JsonNode getTargetNode(StackGresPgbouncerConfig resource) {
    return super.getTargetNode(resource).get("pgbouncer.ini");
  }

  @Override
  public List<JsonPatchOperation> mutate(PgBouncerReview review) {
    return mutate(PG_BOUNCER_CONFIG_POINTER, review.getRequest().getObject());
  }
}
