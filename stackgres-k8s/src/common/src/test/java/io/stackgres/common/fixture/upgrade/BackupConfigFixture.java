/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.upgrade;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class BackupConfigFixture extends Fixture<StackGresBackupConfig> {

  public BackupConfigFixture loadDefault() {
    fixture = readFromJson(UPGRADE_SGBACKUPCONFIG_JSON);
    return this;
  }

  public StackGresBackupConfigBuilder getBuilder() {
    return new StackGresBackupConfigBuilder(fixture);
  }

}
