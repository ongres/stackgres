/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class PgBouncerDefaultValuesMutator
    extends DefaultValuesMutator<StackGresPoolingConfig, PoolingReview>
    implements PgBouncerMutator {

  @Override
  public JsonNode getSourceNode(StackGresPoolingConfig resource) {
    var source = (ObjectNode) toNode(resource)
        .get("status").get("pgBouncer").get("defaultParameters");
    List.copyOf(PgBouncerBlocklist.getBlocklistParameters())
        .forEach(source::remove);
    return source;
  }

  @Override
  public JsonNode getTargetNode(StackGresPoolingConfig resource) {
    return toNode(resource)
        .get("spec").get("pgBouncer").get("pgbouncer.ini").get("pgbouncer");
  }

  @Override
  public List<JsonPatchOperation> mutate(PoolingReview review) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return List.of();
    }

    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

    StackGresPoolingConfig pgBouncerConfig = review.getRequest().getObject();
    StackGresPoolingConfigSpec spec = pgBouncerConfig.getSpec();
    if (spec == null) {
      spec = new StackGresPoolingConfigSpec();
      pgBouncerConfig.setSpec(spec);
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER.parent().parent().parent(),
          FACTORY.objectNode()));
    }
    StackGresPoolingConfigPgBouncer pgBouncer = spec.getPgBouncer();
    if (pgBouncer == null) {
      pgBouncer = new StackGresPoolingConfigPgBouncer();
      spec.setPgBouncer(pgBouncer);
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER.parent().parent(),
          FACTORY.objectNode()));
    }
    var pgBouncerIni = pgBouncer.getPgbouncerIni();
    if (pgBouncerIni == null) {
      pgBouncerIni = new StackGresPoolingConfigPgBouncerPgbouncerIni();
      pgBouncer.setPgbouncerIni(pgBouncerIni);
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER.parent(), FACTORY.objectNode()));
    }

    if (pgBouncerIni.getParameters() == null) {
      pgBouncerIni.setParameters(Map.of());
      operations.add(new AddOperation(PG_BOUNCER_CONFIG_POINTER, FACTORY.objectNode()));
    }

    operations.addAll(mutate(PG_BOUNCER_CONFIG_POINTER, pgBouncerConfig));
    return operations.build();
  }

}
