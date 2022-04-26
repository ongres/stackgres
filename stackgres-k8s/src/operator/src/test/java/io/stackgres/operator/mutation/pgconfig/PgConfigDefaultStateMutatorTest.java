/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.mutation.DefaultStateMutator;
import io.stackgres.operator.mutation.DefaultStateMutatorTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgConfigDefaultStateMutatorTest
    extends DefaultStateMutatorTest<StackGresPostgresConfig, PgConfigReview> {

  @Override
  protected DefaultStateMutator<StackGresPostgresConfig, PgConfigReview> getMutatorInstance() {
    return new PgConfigDefaultStateMutator();
  }

  @Override
  protected PgConfigReview getEmptyReview() {
    PgConfigReview review = JsonUtil
        .readFromJson("pgconfig_allow_request/valid_pgconfig.json", PgConfigReview.class);
    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());
    return review;
  }

  @Override
  protected PgConfigReview getDefaultReview() {
    return JsonUtil
        .readFromJson("pgconfig_allow_request/valid_pgconfig.json", PgConfigReview.class);
  }

  @Override
  protected StackGresPostgresConfig getDefaultResource() {
    return JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("status").get("defaultParameters");
  }

  @Override
  protected int getMissingParentsCount() {
    return 1;
  }

  @Override
  protected Map<String, String> getConfigParameters(StackGresPostgresConfig resource) {
    return resource.getStatus().getDefaultParameters();
  }

}
