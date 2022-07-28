/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class PgConfigDefaultValuesMutator
    extends DefaultValuesMutator<StackGresPostgresConfig, PgConfigReview> {

  public static PgConfigDefaultValuesMutator create(
      DefaultCustomResourceFactory<StackGresPostgresConfig> factory,
      ObjectMapper objectMapper) {
    PgConfigDefaultValuesMutator mutator = new PgConfigDefaultValuesMutator();
    mutator.setFactory(factory);
    mutator.setObjectMapper(objectMapper);
    mutator.init();
    return mutator;
  }

  @Override
  public JsonNode getSourceNode(StackGresPostgresConfig resource) {
    var source = (ObjectNode) toNode(resource)
        .get("status").get("defaultParameters");
    List.copyOf(PostgresBlocklist.getBlocklistParameters())
        .forEach(source::remove);
    return source;
  }

  @Override
  public JsonNode getTargetNode(StackGresPostgresConfig resource) {
    return toNode(resource)
        .get("spec").get("postgresql.conf");
  }

  @Override
  public List<JsonPatchOperation> mutate(PgConfigReview review) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return List.of();
    }

    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    StackGresPostgresConfigSpec spec = pgConfig.getSpec();
    if (spec == null) {
      spec = new StackGresPostgresConfigSpec();
      pgConfig.setSpec(spec);
      operations.add(new AddOperation(PgConfigMutator.PG_CONFIG_POINTER.parent(),
          FACTORY.objectNode()));
    }
    if (spec.getPostgresqlConf() == null) {
      pgConfig.getSpec().setPostgresqlConf(Map.of());
      operations.add(new AddOperation(PgConfigMutator.PG_CONFIG_POINTER,
          FACTORY.objectNode()));
    }

    operations.addAll(mutate(PgConfigMutator.PG_CONFIG_POINTER, pgConfig));
    return operations.build();
  }

}
