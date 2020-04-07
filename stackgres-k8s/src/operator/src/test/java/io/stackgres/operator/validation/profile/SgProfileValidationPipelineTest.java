/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@QuarkusTest
@DisabledIfEnvironmentVariable(named = "SKIP_QUARKUS_TEST", matches = "true")
public class SgProfileValidationPipelineTest extends ValidationPipelineTest<StackGresProfile, SgProfileReview> {

  @Inject
  public SgProfileValidationPipeline pipeline;

  @Override
  public SgProfileReview getConstraintViolatingReview() {
    SgProfileReview review = getValidReview();

    review.getRequest().getObject().getSpec().setMemory("");

    return review;
  }

  private SgProfileReview getValidReview() {
    return JsonUtil.readFromJson("sgprofile_allow_request/create.json",
        SgProfileReview.class);
  }

  @Override
  public ValidationPipeline<SgProfileReview> getPipeline() {
    return pipeline;
  }

}