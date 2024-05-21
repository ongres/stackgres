/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.common.StackGresShardedBackupReviewBuilder;

public class ShardedBackupReviewFixture extends VersionedFixture<StackGresShardedBackupReview> {

  public static ShardedBackupReviewFixture fixture() {
    return new ShardedBackupReviewFixture();
  }

  public ShardedBackupReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_SHARDED_BACKUP_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public ShardedBackupReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_SHARDED_BACKUP_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public ShardedBackupReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_SHARDED_BACKUP_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public StackGresShardedBackupReviewBuilder getBuilder() {
    return new StackGresShardedBackupReviewBuilder(fixture);
  }

}
