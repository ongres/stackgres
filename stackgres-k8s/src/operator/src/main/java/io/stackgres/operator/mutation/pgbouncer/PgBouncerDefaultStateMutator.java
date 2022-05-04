/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.mutation.DefaultStateMutator;

@ApplicationScoped
public class PgBouncerDefaultStateMutator
    extends DefaultStateMutator<StackGresPoolingConfig, PoolingReview>
    implements PgBouncerMutator {

  @Override
  public JsonNode getTargetNode(StackGresPoolingConfig resource) {
    return PG_BOUNCER_DEFAULT_PARAMETERS_POINTER.get(super.getTargetNode(resource));
  }

  @Override
  public List<JsonPatchOperation> mutate(PoolingReview review) {
    List<JsonPatchOperation> operations = new ArrayList<>();
    StackGresPoolingConfig pgBouncerConfig = review.getRequest().getObject();
    if (pgBouncerConfig.getStatus() == null) {
      pgBouncerConfig.setStatus(new StackGresPoolingConfigStatus());
      operations.add(buildAddOperation(
          PG_BOUNCER_DEFAULT_PARAMETERS_POINTER.parent().parent(), FACTORY.objectNode()));
    }
    if (pgBouncerConfig.getStatus().getPgBouncer() == null) {
      pgBouncerConfig.getStatus().setPgBouncer(new StackGresPoolingConfigPgBouncerStatus());
      operations.add(buildAddOperation(
          PG_BOUNCER_DEFAULT_PARAMETERS_POINTER.parent(), FACTORY.objectNode()));
    }
    operations.addAll(mutate(PG_BOUNCER_DEFAULT_PARAMETERS_POINTER, pgBouncerConfig));
    return operations;
  }

  @Override
  protected Map<String, String> getParametersNode(StackGresPoolingConfig incomingResource) {
    return Optional.ofNullable(incomingResource.getStatus())
        .map(StackGresPoolingConfigStatus::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncerStatus::getDefaultParameters)
        .orElseGet(Map::of);
  }
}
