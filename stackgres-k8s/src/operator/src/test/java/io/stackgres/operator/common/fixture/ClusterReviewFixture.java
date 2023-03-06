/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.StackGresClusterReviewBuilder;

public class ClusterReviewFixture extends VersionedFixture<StackGresClusterReview> {

  public static ClusterReviewFixture fixture() {
    return new ClusterReviewFixture();
  }

  public ClusterReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_CLUSTER_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_CLUSTER_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_CLUSTER_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public ClusterReviewFixture loadBackupConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_BACKUP_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadConnectionPoolingConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_CONNECTION_POOLING_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadDistributedLogsUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_DISTRIBUTED_LOGS_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadInvalidCreationEmptyPgVersion() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_EMPTY_PG_VERSION_JSON);
    return this;
  }

  public ClusterReviewFixture loadInvalidCreationNoPgVersion() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_NO_PG_VERSION_JSON);
    return this;
  }

  public ClusterReviewFixture loadInvalidCreationPgVersion() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_PG_VERSION_JSON);
    return this;
  }

  public ClusterReviewFixture loadInvalidCreationZeroInstances() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_INVALID_CREATION_ZERO_INSTANCES_JSON);
    return this;
  }

  public ClusterReviewFixture loadMajorPostgresVersionUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_MAJOR_POSTGRES_VERSION_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadMinorPostgresVersionUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_MINOR_POSTGRES_VERSION_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadPostgresConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_POSTGRES_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadProfileConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_PROFILE_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadRestoreConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_RESTORE_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadPatroniInitialConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_PATRONI_INITIAL_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadScriptsConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_SCRIPTS_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadSslUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_SSL_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadStorageClassConfigUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_STORAGE_CLASS_CONFIG_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadWrongMajorPostgresVersionUpdate() {
    fixture = readFromJson(
        STACKGRES_CLUSTER_ADMISSION_REVIEW_WRONG_MAJOR_POSTGRES_VERSION_UPDATE_JSON);
    return this;
  }

  public ClusterReviewFixture loadCreateWithManagedSql() {
    fixture = readFromJson(STACKGRES_CLUSTER_ADMISSION_REVIEW_CREATE_WITH_MANAGED_SQL_JSON);
    return this;
  }

  public ClusterReviewFixture loadUpdateWithManagedSql() {
    fixture = readFromJson(STACKGRES_CLUSTER_ADMISSION_REVIEW_UPDATE_WITH_MANAGED_SQL_JSON);
    return this;
  }

  public StackGresClusterReviewBuilder getBuilder() {
    return new StackGresClusterReviewBuilder(fixture);
  }

}
