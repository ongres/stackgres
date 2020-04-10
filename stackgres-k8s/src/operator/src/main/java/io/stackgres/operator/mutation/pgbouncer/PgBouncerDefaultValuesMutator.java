/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class PgBouncerDefaultValuesMutator
    extends DefaultValuesMutator<StackGresPoolingConfig, PoolingReview>
    implements PgBouncerMutator {

  @Override
  public JsonNode getTargetNode(StackGresPoolingConfig resource) {
    return super.getTargetNode(resource).get("pgBouncer").get("pgbouncer.ini");
  }

  @Override
  public List<JsonPatchOperation> mutate(PoolingReview review) {
    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

    StackGresPoolingConfig pgBouncerConfig = review.getRequest().getObject();
    StackGresPoolingConfigSpec spec = pgBouncerConfig.getSpec();
    if (spec == null) {
      spec = new StackGresPoolingConfigSpec();
      pgBouncerConfig.setSpec(spec);
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER.parent().parent(),
          FACTORY.objectNode()));
    }
    StackGresPoolingConfigPgBouncer pgBouncer = spec.getPgBouncer();
    if (pgBouncer == null) {
      pgBouncer = new StackGresPoolingConfigPgBouncer();
      spec.setPgBouncer(pgBouncer);
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER.parent(), FACTORY.objectNode()));
    }
    if (pgBouncer.getPgbouncerConf() == null) {
      pgBouncer.setPgbouncerConf(ImmutableMap.of());
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER, FACTORY.objectNode()));
    }

    operations.addAll(mutate(PG_BOUNCER_CONFIG_POINTER, pgBouncerConfig));
    return operations.build();
  }
}
