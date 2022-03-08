/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.backupconfig;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.testutil.fixture.Fixture;

public class BackupConfigFixture extends Fixture<StackGresBackupConfig> {

  public BackupConfigFixture loadDefault() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_DEFAULT_JSON);
    return this;
  }

}
