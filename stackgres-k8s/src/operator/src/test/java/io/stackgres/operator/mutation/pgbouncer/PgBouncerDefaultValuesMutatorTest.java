/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgBouncerDefaultValuesMutatorTest extends DefaultValuesMutatorTest<StackGresPgbouncerConfig, PgBouncerReview> {

  @Override
  protected DefaultValuesMutator<StackGresPgbouncerConfig, PgBouncerReview> getMutatorInstance() {
    return new PgBouncerDefaultValuesMutator();
  }

  @Override
  protected PgBouncerReview getEmptyReview() {
    PgBouncerReview review = JsonUtil
        .readFromJson("pgbouncer_allow_request/create.json", PgBouncerReview.class);
    review.getRequest().getObject().getSpec().setPgbouncerConf(new HashMap<>());
    return review;
  }

  @Override
  protected PgBouncerReview getDefaultReview() {
    return JsonUtil
        .readFromJson("pgbouncer_allow_request/create.json", PgBouncerReview.class);
  }

  @Override
  protected StackGresPgbouncerConfig getDefaultResource() {
    return JsonUtil.readFromJson("pgbouncer_config/default.json",
        StackGresPgbouncerConfig.class);
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("spec").get("pgbouncer.ini");
  }


}