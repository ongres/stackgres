/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.StackGresObjectStorageReviewBuilder;

public class ObjectStorageReviewFixture extends VersionedFixture<StackGresObjectStorageReview> {

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

  public StackGresObjectStorageReviewBuilder getBuilder() {
    return new StackGresObjectStorageReviewBuilder(fixture);
  }

}
