/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class PgConfigDefaultValuesMutator
    extends DefaultValuesMutator<StackGresPostgresConfig, PgConfigReview>
    implements PgConfigMutator {

  @Override
  public JsonNode getTargetNode(StackGresPostgresConfig resource) {
    return super.getTargetNode(resource).get("postgresql.conf");
  }

  @Override
  public List<JsonPatchOperation> mutate(PgConfigReview review) {

    return mutate(PG_CONFIG_POINTER, review.getRequest().getObject());

  }
}
