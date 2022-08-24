/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.poolingconfig;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class PoolingConfigFixture extends VersionedFixture<StackGresPoolingConfig> {

  public PoolingConfigFixture loadDefault() {
    fixture = readFromJson(STACKGRES_POOLING_CONFIG_DEFAULT_JSON);
    return this;
  }

  public StackGresPoolingConfigBuilder getBuilder() {
    return new StackGresPoolingConfigBuilder(fixture);
  }

}
