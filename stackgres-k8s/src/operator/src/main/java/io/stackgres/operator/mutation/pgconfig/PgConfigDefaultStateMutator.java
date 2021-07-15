/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.mutation.DefaultStateMutator;

public class PgConfigDefaultStateMutator
    extends DefaultStateMutator<StackGresPostgresConfig, PgConfigReview>
    implements PgConfigMutator {

  @Override
  public JsonNode getTargetNode(StackGresPostgresConfig resource) {
    return PG_CONFIG_DEFAULT_PARAMETERS_POINTER.get(super.getTargetNode(resource));
  }

  @Override
  public List<JsonPatchOperation> mutate(PgConfigReview review) {
    List<JsonPatchOperation> operations = new ArrayList<>();
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    if (pgConfig.getStatus() == null) {
      pgConfig.setStatus(new StackGresPostgresConfigStatus());
      operations.add(buildAddOperation(
          PG_CONFIG_DEFAULT_PARAMETERS_POINTER.parent(), MAPPER.createObjectNode()));
    }
    operations.addAll(mutate(PG_CONFIG_DEFAULT_PARAMETERS_POINTER, pgConfig));
    return operations;
  }

  @Override
  protected Map<String, String> getParametersNode(StackGresPostgresConfig incomingResource) {
    return Optional.ofNullable(incomingResource.getSpec())
        .map(StackGresPostgresConfigSpec::getPostgresqlConf)
        .orElse(ImmutableMap.of());
  }

}
