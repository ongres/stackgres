/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operator.common.ConfigReviewBuilder;

public class ConfigReviewFixture extends VersionedFixture<ConfigReview> {

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

  public ConfigReviewBuilder getBuilder() {
    return new ConfigReviewBuilder(fixture);
  }

}
