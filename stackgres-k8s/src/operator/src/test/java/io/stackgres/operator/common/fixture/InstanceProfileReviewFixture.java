/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.SgProfileReviewBuilder;

public class InstanceProfileReviewFixture extends VersionedFixture<SgProfileReview> {

  public static InstanceProfileReviewFixture fixture() {
    return new InstanceProfileReviewFixture();
  }

  public InstanceProfileReviewFixture loadCreate() {
    fixture = readFromJson(
        STACKGRES_INSTANCE_PROFILE_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public InstanceProfileReviewFixture loadUpdate() {
    fixture = readFromJson(
        STACKGRES_INSTANCE_PROFILE_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public InstanceProfileReviewFixture loadDelete() {
    fixture = readFromJson(
        STACKGRES_INSTANCE_PROFILE_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public SgProfileReviewBuilder getBuilder() {
    return new SgProfileReviewBuilder(fixture);
  }

}
