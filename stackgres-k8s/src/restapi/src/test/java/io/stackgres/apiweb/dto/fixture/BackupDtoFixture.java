/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.testutil.fixture.Fixture;

public class BackupDtoFixture extends Fixture<BackupDto> {

  public BackupDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_BACKUP_DTO_JSON);
    return this;
  }

}
