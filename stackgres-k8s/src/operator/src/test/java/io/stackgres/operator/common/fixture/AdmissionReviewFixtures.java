/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

public interface AdmissionReviewFixtures {

  static BackupConfigReviewFixture backupConfig() {
    return new BackupConfigReviewFixture();
  }

  static BackupReviewFixture backup() {
    return new BackupReviewFixture();
  }

  static ClusterReviewFixture cluster() {
    return new ClusterReviewFixture();
  }

  static DbOpsReviewFixture dbOps() {
    return new DbOpsReviewFixture();
  }

  static DistributedLogsReviewFixture distributedLogs() {
    return new DistributedLogsReviewFixture();
  }

  static InstanceProfileReviewFixture instanceProfile() {
    return new InstanceProfileReviewFixture();
  }

  static PoolingConfigReviewFixture poolingConfig() {
    return new PoolingConfigReviewFixture();
  }

  static PostgresConfigReviewFixture postgresConfig() {
    return new PostgresConfigReviewFixture();
  }

  static ObjectStorageReviewFixture objectStorage() {
    return new ObjectStorageReviewFixture();
  }

  static ScriptReviewFixture script() {
    return new ScriptReviewFixture();
  }

  static ShardedClusterReviewFixture shardedCluster() {
    return new ShardedClusterReviewFixture();
  }

  static ShardedBackupReviewFixture shardedBackup() {
    return new ShardedBackupReviewFixture();
  }

  static ShardedDbOpsReviewFixture shardedDbOps() {
    return new ShardedDbOpsReviewFixture();
  }

  static ConfigReviewFixture config() {
    return new ConfigReviewFixture();
  }

}
