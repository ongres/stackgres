/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.common.StackGresPoolingConfigReviewBuilder;

public class PoolingConfigReviewFixture extends VersionedFixture<StackGresPoolingConfigReview> {

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

  public StackGresPoolingConfigReviewBuilder getBuilder() {
    return new StackGresPoolingConfigReviewBuilder(fixture);
  }

}
