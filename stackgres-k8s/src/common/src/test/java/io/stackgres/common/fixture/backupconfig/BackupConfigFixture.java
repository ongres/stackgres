/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.backupconfig;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class BackupConfigFixture extends VersionedFixture<StackGresBackupConfig> {

  public BackupConfigFixture loadDefault() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_DEFAULT_JSON);
    return this;
  }

  public StackGresBackupConfigBuilder getBuilder() {
    return new StackGresBackupConfigBuilder(fixture);
  }

}
