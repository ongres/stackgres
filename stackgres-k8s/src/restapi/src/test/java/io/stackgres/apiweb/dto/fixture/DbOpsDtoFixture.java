/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.testutil.fixture.Fixture;

public class DbOpsDtoFixture extends Fixture<DbOpsDto> {

  public DbOpsDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_DB_OPS_DTO_JSON);
    return this;
  }

}
