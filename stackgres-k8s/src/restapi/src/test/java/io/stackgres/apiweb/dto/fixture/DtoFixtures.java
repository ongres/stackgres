/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

public interface DtoFixtures {

  static BackupDtoFixture backup() {
    return new BackupDtoFixture();
  }

  static ClusterDtoFixture cluster() {
    return new ClusterDtoFixture();
  }

  static DbOpsDtoFixture dbOps() {
    return new DbOpsDtoFixture();
  }

  static DistributedLogsDtoFixture distributedLogs() {
    return new DistributedLogsDtoFixture();
  }

  static InstanceProfileDtoFixture instanceProfile() {
    return new InstanceProfileDtoFixture();
  }

  static PoolingConfigDtoFixture poolingConfig() {
    return new PoolingConfigDtoFixture();
  }

  static PostgresConfigDtoFixture postgresConfig() {
    return new PostgresConfigDtoFixture();
  }

  static ObjectStorageDtoFixture objectStorage() {
    return new ObjectStorageDtoFixture();
  }

  static ScriptDtoFixture script() {
    return new ScriptDtoFixture();
  }

  static ShardedClusterDtoFixture shardedCluster() {
    return new ShardedClusterDtoFixture();
  }

  static ShardedBackupDtoFixture shardedBackup() {
    return new ShardedBackupDtoFixture();
  }

  static ShardedDbOpsDtoFixture shardedDbOps() {
    return new ShardedDbOpsDtoFixture();
  }

  static StreamDtoFixture stream() {
    return new StreamDtoFixture();
  }

  static UserDtoFixture user() {
    return new UserDtoFixture();
  }

}
