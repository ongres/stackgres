/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.config;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class ConfigFixture extends VersionedFixture<StackGresConfig> {

  public ConfigFixture loadDefault() {
    fixture = readFromJson(STACKGRES_CONFIG_DEFAULT_JSON);
    return this;
  }

  public StackGresConfigBuilder getBuilder() {
    return new StackGresConfigBuilder(fixture);
  }

}
