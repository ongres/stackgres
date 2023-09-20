/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.Map;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class PoolingValidationPipelineTest
    extends ValidationPipelineTest<StackGresPoolingConfig, PoolingReview> {

  @Inject
  public PoolingValidationPipeline pipeline;

  @Override
  public PoolingReview getConstraintViolatingReview() {
    PoolingReview review = getValidReview();

    review.getRequest().getObject().getSpec().getPgBouncer().getPgbouncerIni()
        .setPgbouncer(Map.of());

    return review;
  }

  private PoolingReview getValidReview() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

  @Override
  public ValidationPipeline<PoolingReview> getPipeline() {
    return pipeline;
  }
}
