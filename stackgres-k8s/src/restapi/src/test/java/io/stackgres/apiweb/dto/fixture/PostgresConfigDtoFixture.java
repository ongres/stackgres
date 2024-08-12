/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.testutil.fixture.Fixture;

public class PostgresConfigDtoFixture extends Fixture<PostgresConfigDto> {

  public PostgresConfigDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_POSTGRES_CONFIG_DTO_JSON);
    return this;
  }

}
