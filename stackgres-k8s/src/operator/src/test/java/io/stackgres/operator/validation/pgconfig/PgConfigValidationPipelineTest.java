/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.HashMap;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class PgConfigValidationPipelineTest
    extends ValidationPipelineTest<StackGresPostgresConfig, PgConfigReview> {

  @Inject
  public PgConfigValidationPipeline pipeline;

  @Override
  public PgConfigReview getConstraintViolatingReview() {
    PgConfigReview review = getValidReview();

    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());

    return review;
  }

  private PgConfigReview getValidReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  public ValidationPipeline<PgConfigReview> getPipeline() {
    return pipeline;
  }
}
