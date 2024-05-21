/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.common.StackGresStreamReviewBuilder;

public class StreamReviewFixture extends VersionedFixture<StackGresStreamReview> {

  public static StreamReviewFixture fixture() {
    return new StreamReviewFixture();
  }

  public StreamReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_STREAM_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public StreamReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_STREAM_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public StreamReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_STREAM_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public StackGresStreamReviewBuilder getBuilder() {
    return new StackGresStreamReviewBuilder(fixture);
  }

}
