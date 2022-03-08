/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.backupconfig;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.testutil.fixture.Fixture;

public class BackupConfigListFixture extends Fixture<StackGresBackupConfigList> {

  public BackupConfigListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_LIST_JSON);
    return this;
  }

  public BackupConfigListFixture withJustFirstElement() {
    if (fixture.getItems() != null && !fixture.getItems().isEmpty()) {
      fixture.setItems(fixture.getItems().subList(0, 1));
    }
    return this;
  }

}
