/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.StackGresPostgresConfigReviewBuilder;

public class PostgresConfigReviewFixture extends VersionedFixture<StackGresPostgresConfigReview> {

  public static PostgresConfigReviewFixture fixture() {
    return new PostgresConfigReviewFixture();
  }

  public PostgresConfigReviewFixture loadCreate() {
    fixture = readFromJson(
        STACKGRES_POSTGRES_CONFIG_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public PostgresConfigReviewFixture loadUpdate() {
    fixture = readFromJson(
        STACKGRES_POSTGRES_CONFIG_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public PostgresConfigReviewFixture loadDelete() {
    fixture = readFromJson(
        STACKGRES_POSTGRES_CONFIG_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public StackGresPostgresConfigReviewBuilder getBuilder() {
    return new StackGresPostgresConfigReviewBuilder(fixture);
  }

}
