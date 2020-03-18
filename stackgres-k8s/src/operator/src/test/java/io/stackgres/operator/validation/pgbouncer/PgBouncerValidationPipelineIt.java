/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import java.util.HashMap;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;

@QuarkusTest
public class PgBouncerValidationPipelineIt
    extends ValidationPipelineTest<StackGresPgbouncerConfig, PgBouncerReview> {

  @Override
  public PgBouncerReview getConstraintViolatingReview() {
    PgBouncerReview review = getValidReview();

    review.getRequest().getObject().getSpec().setPgbouncerConf(new HashMap<>());

    return review;
  }

  private PgBouncerReview getValidReview() {
    return JsonUtil.readFromJson("pgbouncer_allow_request/create.json",
        PgBouncerReview.class);
  }
}