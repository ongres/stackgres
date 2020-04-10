/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgBouncerDefaultValuesMutatorTest extends DefaultValuesMutatorTest<StackGresPoolingConfig, PoolingReview> {

  @Override
  protected DefaultValuesMutator<StackGresPoolingConfig, PoolingReview> getMutatorInstance() {
    return new PgBouncerDefaultValuesMutator();
  }

  @Override
  protected PoolingReview getEmptyReview() {
    PoolingReview review = JsonUtil
        .readFromJson("pooling_allow_request/create.json", PoolingReview.class);
    review.getRequest().getObject().getSpec().getPgBouncer().setPgbouncerConf(new HashMap<>());
    return review;
  }

  @Override
  protected PoolingReview getDefaultReview() {
    return JsonUtil
        .readFromJson("pooling_allow_request/create.json", PoolingReview.class);
  }

  @Override
  protected StackGresPoolingConfig getDefaultResource() {
    return JsonUtil.readFromJson("pooling_config/default.json",
        StackGresPoolingConfig.class);
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("spec").get("pgBouncer").get("pgbouncer.ini");
  }


}