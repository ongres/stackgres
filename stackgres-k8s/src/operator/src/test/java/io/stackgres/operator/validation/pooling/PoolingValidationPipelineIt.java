/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.HashMap;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;

@QuarkusTest
public class PoolingValidationPipelineIt
    extends ValidationPipelineTest<StackGresPoolingConfig, PoolingReview> {

  @Override
  public PoolingReview getConstraintViolatingReview() {
    PoolingReview review = getValidReview();

    review.getRequest().getObject().getSpec().getPgBouncer().setPgbouncerConf(new HashMap<>());

    return review;
  }

  private PoolingReview getValidReview() {
    return JsonUtil.readFromJson("pooling_allow_request/create.json",
        PoolingReview.class);
  }
}