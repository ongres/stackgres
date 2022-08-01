/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.upgrade;

public interface Upgrade {

  default BackupConfigFixture backupConfig() {
    return new BackupConfigFixture();
  }

  default ClusterFixture cluster() {
    return new ClusterFixture();
  }

  default InstanceProfileFixture instanceProfile() {
    return new InstanceProfileFixture();
  }

  default PoolingConfigFixture poolingConfig() {
    return new PoolingConfigFixture();
  }

  default PostgresConfigFixture postgresConfig() {
    return new PostgresConfigFixture();
  }

  default JsonPatroniConfigFixture jsonPatroniConfig() {
    return new JsonPatroniConfigFixture();
  }

}
