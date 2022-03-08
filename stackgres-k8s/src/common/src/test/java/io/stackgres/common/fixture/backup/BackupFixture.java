/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.backup;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.testutil.fixture.Fixture;

public class BackupFixture extends Fixture<StackGresBackup> {

  public BackupFixture loadDefault() {
    fixture = readFromJson(STACKGRES_BACKUP_DEFAULT_JSON);
    return this;
  }

}
