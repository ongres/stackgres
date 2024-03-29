/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.upgrade;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class PoolingConfigFixture extends Fixture<StackGresPoolingConfig> {

  public PoolingConfigFixture loadDefault() {
    fixture = readFromJson(UPGRADE_SGPOOLCONFIG_JSON);
    return this;
  }

  public StackGresPoolingConfigBuilder getBuilder() {
    return new StackGresPoolingConfigBuilder(fixture);
  }

}
