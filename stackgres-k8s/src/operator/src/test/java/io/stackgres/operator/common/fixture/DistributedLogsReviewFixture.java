/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.testutil.fixture.Fixture;

public class DistributedLogsReviewFixture extends Fixture<StackGresDistributedLogsReview> {

  public static DistributedLogsReviewFixture fixture() {
    return new DistributedLogsReviewFixture();
  }

  public DistributedLogsReviewFixture loadCreate() {
    fixture = readFromJson(
        STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public DistributedLogsReviewFixture loadUpdate() {
    fixture = readFromJson(
        STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public DistributedLogsReviewFixture loadProfileConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_PROFILE_CONFIG_UPDATE_JSON);
    return this;
  }

  public DistributedLogsReviewFixture loadDelete() {
    fixture = readFromJson(
        STACKGRES_DISTRIBUTED_LOGS_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

}
