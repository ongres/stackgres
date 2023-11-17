/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
class ScriptValidationPipelineTest
    extends ValidationPipelineTest<StackGresScript, StackGresScriptReview> {

  @Inject
  public ScriptValidationPipeline pipeline;

  @Override
  public StackGresScriptReview getConstraintViolatingReview() {
    final StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadCreate().get();
    return review;
  }

  @Override
  public ValidationPipeline<StackGresScriptReview> getPipeline() {
    return pipeline;
  }

}
