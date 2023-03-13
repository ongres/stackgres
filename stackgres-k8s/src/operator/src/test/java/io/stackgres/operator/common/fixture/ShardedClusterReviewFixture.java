/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.StackGresShardedClusterReviewBuilder;

public class ShardedClusterReviewFixture extends VersionedFixture<StackGresShardedClusterReview> {

  public static ShardedClusterReviewFixture fixture() {
    return new ShardedClusterReviewFixture();
  }

  public ShardedClusterReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadConnectionPoolingConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_CONNECTION_POOLING_CONFIG_UPDATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadInvalidCreationEmptyPgVersion() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_EMPTY_PG_VERSION_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadInvalidCreationNoPgVersion() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_NO_PG_VERSION_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadInvalidCreationPgVersion() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_PG_VERSION_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadInvalidCreationZeroInstances() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_ZERO_INSTANCES_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadMajorPostgresVersionUpdate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_MAJOR_POSTGRES_VERSION_UPDATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadMinorPostgresVersionUpdate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_MINOR_POSTGRES_VERSION_UPDATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadPostgresConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_POSTGRES_CONFIG_UPDATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadProfileConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_PROFILE_CONFIG_UPDATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadStorageClassConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_STORAGE_CLASS_CONFIG_UPDATE_JSON);
    return this;
  }

  public ShardedClusterReviewFixture loadWrongMajorPostgresVersionUpdate() {
    fixture = readFromJson(
        STACKGRES_SHARDED_CLUSTER_ADMISSION_REVIEW_WRONG_MAJOR_POSTGRES_VERSION_UPDATE_JSON);
    return this;
  }

  public StackGresShardedClusterReviewBuilder getBuilder() {
    return new StackGresShardedClusterReviewBuilder(fixture);
  }

}
