/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.common.StackGresDbOpsReviewBuilder;

public class DbOpsReviewFixture extends VersionedFixture<StackGresDbOpsReview> {

  public static DbOpsReviewFixture fixture() {
    return new DbOpsReviewFixture();
  }

  public DbOpsReviewFixture loadRestartCreate() {
    fixture = readFromJson(STACKGRES_DB_OPS_ADMISSION_REVIEW_RESTART_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadMajorVersionUpgradeCreate() {
    fixture = readFromJson(
        STACKGRES_DB_OPS_ADMISSION_REVIEW_MAJOR_VERSION_UPGRADE_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadMinorVersionUpgradeCreate() {
    fixture = readFromJson(
        STACKGRES_DB_OPS_ADMISSION_REVIEW_MINOR_VERSION_UPGRADE_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadPgbenchCreate() {
    fixture = readFromJson(
        STACKGRES_DB_OPS_ADMISSION_REVIEW_PGBENCH_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadSamplingCreate() {
    fixture = readFromJson(
        STACKGRES_DB_OPS_ADMISSION_REVIEW_SAMPLING_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadRepackCreate() {
    fixture = readFromJson(
        STACKGRES_DB_OPS_ADMISSION_REVIEW_REPACK_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadSecurityUpgradeCreate() {
    fixture = readFromJson(
        STACKGRES_DB_OPS_ADMISSION_REVIEW_SECURITY_UPGRADE_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadVacuumCreate() {
    fixture = readFromJson(
        STACKGRES_DB_OPS_ADMISSION_REVIEW_VACUUM_CREATE_JSON);
    return this;
  }

  public DbOpsReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_DB_OPS_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public StackGresDbOpsReviewBuilder getBuilder() {
    return new StackGresDbOpsReviewBuilder(fixture);
  }

}
