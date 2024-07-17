/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresBackupReview;
import io.stackgres.operator.common.StackGresBackupReviewBuilder;

public class BackupReviewFixture extends VersionedFixture<StackGresBackupReview> {

  public static BackupReviewFixture fixture() {
    return new BackupReviewFixture();
  }

  public BackupReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_BACKUP_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public BackupReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_BACKUP_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public BackupReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_BACKUP_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public StackGresBackupReviewBuilder getBuilder() {
    return new StackGresBackupReviewBuilder(fixture);
  }

}
