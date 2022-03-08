/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.postgresconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.testutil.fixture.Fixture;

public class PostgresConfigListFixture extends Fixture<StackGresPostgresConfigList> {

  public PostgresConfigListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_POSTGRES_CONFIG_LIST_JSON);
    return this;
  }

}
