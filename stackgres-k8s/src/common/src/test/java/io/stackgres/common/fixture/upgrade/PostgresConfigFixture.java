/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.upgrade;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.testutil.fixture.Fixture;

public class PostgresConfigFixture extends Fixture<StackGresPostgresConfig> {

  public PostgresConfigFixture loadDefault() {
    fixture = readFromJson(UPGRADE_SGPGCONFIG_JSON);
    return this;
  }

}
