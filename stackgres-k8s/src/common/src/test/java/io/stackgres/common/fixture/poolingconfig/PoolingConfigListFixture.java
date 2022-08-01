/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.poolingconfig;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.testutil.fixture.Fixture;

public class PoolingConfigListFixture extends Fixture<StackGresPoolingConfigList> {

  public static PoolingConfigListFixture fixture() {
    PoolingConfigListFixture fixture = new PoolingConfigListFixture();
    return fixture;
  }

  public PoolingConfigListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_POOLING_CONFIG_LIST_JSON);
    return this;
  }

}
