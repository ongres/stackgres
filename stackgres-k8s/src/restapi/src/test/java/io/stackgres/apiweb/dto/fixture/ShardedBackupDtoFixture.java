/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.testutil.fixture.Fixture;

public class ShardedBackupDtoFixture extends Fixture<ShardedBackupDto> {

  public ShardedBackupDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_BACKUP_DTO_JSON);
    return this;
  }

}
