/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.backup;

import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.testutil.fixture.Fixture;

public class BackupListFixture extends Fixture<StackGresBackupList> {

  public BackupListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_BACKUP_LIST_JSON);
    return this;
  }

  public BackupListFixture withJustFirstElement() {
    if (fixture.getItems() != null && !fixture.getItems().isEmpty()) {
      fixture.setItems(fixture.getItems().subList(0, 1));
    }
    return this;
  }

}
