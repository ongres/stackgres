/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class ObjectStorageValidationPipelineTest
    extends ValidationPipelineTest<StackGresObjectStorage, ObjectStorageReview> {

  @Inject
  public ObjectStorageValidationPipeline pipeline;

  @Override
  public ObjectStorageReview getConstraintViolatingReview() {
    final ObjectStorageReview review =
        JsonUtil.readFromJson("objectstorage_allow_request/create.json",
            ObjectStorageReview.class);
    return review;
  }

  @Override
  public ValidationPipeline<ObjectStorageReview> getPipeline() {
    return pipeline;
  }
}
