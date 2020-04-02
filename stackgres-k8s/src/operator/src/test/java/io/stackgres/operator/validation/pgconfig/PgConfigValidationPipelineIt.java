/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.HashMap;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import org.junit.jupiter.api.Disabled;

@QuarkusTest
@Disabled
class PgConfigValidationPipelineIt
    extends ValidationPipelineTest<StackGresPostgresConfig, PgConfigReview> {

  @Override
  public PgConfigReview getConstraintViolatingReview() {
    PgConfigReview review = getValidReview();

    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());

    return review;
  }

  private PgConfigReview getValidReview() {
    return JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
        PgConfigReview.class);
  }
}