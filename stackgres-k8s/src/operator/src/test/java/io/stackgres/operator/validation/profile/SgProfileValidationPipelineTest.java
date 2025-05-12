/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class SgProfileValidationPipelineTest
    extends ValidationPipelineTest<StackGresProfile, StackGresInstanceProfileReview> {

  @Inject
  public SgProfileValidationPipeline pipeline;

  @Override
  public StackGresInstanceProfileReview getConstraintViolatingReview() {
    StackGresInstanceProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMemory("");
    return review;
  }

  private StackGresInstanceProfileReview getValidReview() {
    return AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }

  @Override
  public ValidationPipeline<StackGresInstanceProfileReview> getPipeline() {
    return pipeline;
  }

}
