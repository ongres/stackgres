/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.common.PoolingReviewBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class PoolingConfigReviewFixture extends Fixture<PoolingReview> {

  public static PoolingConfigReviewFixture fixture() {
    return new PoolingConfigReviewFixture();
  }

  public PoolingConfigReviewFixture loadCreate() {
    fixture = readFromJson(
        STACKGRES_POOLING_CONFIG_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public PoolingConfigReviewFixture loadUpdate() {
    fixture = readFromJson(
        STACKGRES_POOLING_CONFIG_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public PoolingConfigReviewFixture loadDelete() {
    fixture = readFromJson(
        STACKGRES_POOLING_CONFIG_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public PoolingReviewBuilder getBuilder() {
    return new PoolingReviewBuilder(fixture);
  }

}
