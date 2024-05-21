/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresConfigReview;
import io.stackgres.operator.common.StackGresConfigReviewBuilder;

public class ConfigReviewFixture extends VersionedFixture<StackGresConfigReview> {

  public static ConfigReviewFixture fixture() {
    return new ConfigReviewFixture();
  }

  public ConfigReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_CONFIG_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public ConfigReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_CONFIG_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public ConfigReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_CONFIG_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public StackGresConfigReviewBuilder getBuilder() {
    return new StackGresConfigReviewBuilder(fixture);
  }

}
