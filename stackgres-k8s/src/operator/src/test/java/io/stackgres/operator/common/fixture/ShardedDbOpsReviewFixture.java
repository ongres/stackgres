/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.common.StackGresShardedDbOpsReviewBuilder;

public class ShardedDbOpsReviewFixture extends VersionedFixture<StackGresShardedDbOpsReview> {

  public static ShardedDbOpsReviewFixture fixture() {
    return new ShardedDbOpsReviewFixture();
  }

  public ShardedDbOpsReviewFixture loadRestartCreate() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_RESTART_CREATE_JSON);
    return this;
  }

  public ShardedDbOpsReviewFixture loadMajorVersionUpgradeCreate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_MAJOR_VERSION_UPGRADE_CREATE_JSON);
    return this;
  }

  public ShardedDbOpsReviewFixture loadMinorVersionUpgradeCreate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_MINOR_VERSION_UPGRADE_CREATE_JSON);
    return this;
  }

  public ShardedDbOpsReviewFixture loadReshardingCreate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_RESHARDING_CREATE_JSON);
    return this;
  }

  public ShardedDbOpsReviewFixture loadSecurityUpgradeCreate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_SECURITY_UPGRADE_CREATE_JSON);
    return this;
  }

  public ShardedDbOpsReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public StackGresShardedDbOpsReviewBuilder getBuilder() {
    return new StackGresShardedDbOpsReviewBuilder(fixture);
  }

}
