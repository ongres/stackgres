/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.config;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class ConfigValidationPipelineTest
    extends ValidationPipelineTest<StackGresConfig, ConfigReview> {

  @Inject
  public ConfigValidationPipeline pipeline;

  @Override
  public ConfigReview getConstraintViolatingReview() {
    final ConfigReview backupReview = AdmissionReviewFixtures.config().loadCreate().get();
    backupReview.getRequest().getObject().setSpec(null);
    return backupReview;
  }

  @Override
  public ConfigValidationPipeline getPipeline() {
    return pipeline;
  }
}
