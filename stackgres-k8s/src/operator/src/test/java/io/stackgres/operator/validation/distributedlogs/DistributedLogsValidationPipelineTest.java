/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class DistributedLogsValidationPipelineTest
    extends ValidationPipelineTest<StackGresDistributedLogs, StackGresDistributedLogsReview> {

  @Inject
  public DistributedLogsValidationPipeline pipeline;

  @Override
  public StackGresDistributedLogsReview getConstraintViolatingReview() {
    StackGresDistributedLogsReview review = getValidReview();

    review.getRequest().getObject().getSpec().getPersistentVolume().setSize(null);

    return review;
  }

  private StackGresDistributedLogsReview getValidReview() {
    return AdmissionReviewFixtures.distributedLogs().loadCreate().get();
  }

  @Override
  public ValidationPipeline<StackGresDistributedLogsReview> getPipeline() {
    return pipeline;
  }

}
