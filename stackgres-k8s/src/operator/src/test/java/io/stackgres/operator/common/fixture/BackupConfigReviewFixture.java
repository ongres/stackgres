/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.BackupConfigReviewBuilder;

public class BackupConfigReviewFixture extends VersionedFixture<BackupConfigReview> {

  public static BackupConfigReviewFixture fixture() {
    return new BackupConfigReviewFixture();
  }

  public BackupConfigReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public BackupConfigReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public BackupConfigReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public BackupConfigReviewFixture loadInvalidCreationGcsAndS3() {
    fixture = readFromJson(
        STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_INVALID_CREATION_GCS_AND_S3_JSON);
    return this;
  }

  public BackupConfigReviewFixture loadInvalidCreationNoS3() {
    fixture = readFromJson(
        STACKGRES_BACKUP_CONFIG_ADMISSION_REVIEW_INVALID_CREATION_NO_S3_JSON);
    return this;
  }

  public BackupConfigReviewBuilder getBuilder() {
    return new BackupConfigReviewBuilder(fixture);
  }

}
