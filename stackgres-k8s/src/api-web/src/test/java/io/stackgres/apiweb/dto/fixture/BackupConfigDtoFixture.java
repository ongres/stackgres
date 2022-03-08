/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.testutil.fixture.Fixture;

public class BackupConfigDtoFixture extends Fixture<BackupConfigDto> {

  public BackupConfigDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_DTO_JSON);
    return this;
  }

  public BackupConfigDtoFixture loadGoogleIdentityConfig() {
    fixture = readFromJson(STACKGRES_BACKUP_CONFIG_GOOGLE_IDENTITY_CONFIG_JSON);
    return this;
  }

}
