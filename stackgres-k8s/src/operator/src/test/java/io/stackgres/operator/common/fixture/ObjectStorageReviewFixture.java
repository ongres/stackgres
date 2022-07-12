/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.common.ObjectStorageReviewBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class ObjectStorageReviewFixture extends Fixture<ObjectStorageReview> {

  public static ObjectStorageReviewFixture fixture() {
    return new ObjectStorageReviewFixture();
  }

  public ObjectStorageReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public ObjectStorageReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public ObjectStorageReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_OBJECT_STORAGE_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public ObjectStorageReviewBuilder getBuilder() {
    return new ObjectStorageReviewBuilder(fixture);
  }

}
