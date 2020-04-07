/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.HashMap;
import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@QuarkusTest
@DisabledIfEnvironmentVariable(named = "SKIP_QUARKUS_TEST", matches = "true")
public class PoolingValidationPipelineTest
    extends ValidationPipelineTest<StackGresPoolingConfig, PoolingReview> {

  @Inject
  public PoolingValidationPipeline pipeline;

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

  @Override
  public ValidationPipeline<PoolingReview> getPipeline() {
    return pipeline;
  }
}