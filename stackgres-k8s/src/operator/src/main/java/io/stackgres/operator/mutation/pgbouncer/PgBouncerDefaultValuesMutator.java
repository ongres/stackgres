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
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigSpec;

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
    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

    StackGresPgbouncerConfig pgBouncerConfig = review.getRequest().getObject();
    StackGresPgbouncerConfigSpec spec = pgBouncerConfig.getSpec();
    if (spec == null) {
      spec = new StackGresPgbouncerConfigSpec();
      pgBouncerConfig.setSpec(spec);
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER.parent(), FACTORY.objectNode()));
    }
    if (spec.getPgbouncerConf() == null) {
      spec.setPgbouncerConf(ImmutableMap.of());
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER, FACTORY.objectNode()));
    }

    operations.addAll(mutate(PG_BOUNCER_CONFIG_POINTER, pgBouncerConfig));
    return operations.build();
  }
}
