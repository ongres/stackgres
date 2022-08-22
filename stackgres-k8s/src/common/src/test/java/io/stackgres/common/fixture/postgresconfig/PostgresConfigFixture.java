/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.postgresconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class PostgresConfigFixture extends VersionedFixture<StackGresPostgresConfig> {

  public PostgresConfigFixture loadDefault() {
    fixture = readFromJson(STACKGRES_POSTGRES_CONFIG_DEFAULT_JSON);
    return this;
  }

  public StackGresPostgresConfigBuilder getBuilder() {
    return new StackGresPostgresConfigBuilder(fixture);
  }

}
