/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.poolingconfig;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonPoolingConfigFixture extends Fixture<ObjectNode> {

  public JsonPoolingConfigFixture loadFromVersion1() {
    fixture = readFromJsonAsJson(STACKGRES_POOLING_CONFIG_FROM_VERSION1_JSON);
    return this;
  }

  public JsonPoolingConfigFixture loadFromVersion1beta1() {
    fixture = readFromJsonAsJson(STACKGRES_POOLING_CONFIG_FROM_VERSION1BETA1_JSON);
    return this;
  }

  public JsonPoolingConfigFixture loadToVersion1() {
    fixture = readFromJsonAsJson(STACKGRES_POOLING_CONFIG_TO_VERSION1_JSON);
    return this;
  }

  public JsonPoolingConfigFixture loadToVersion1beta1() {
    fixture = readFromJsonAsJson(STACKGRES_POOLING_CONFIG_TO_VERSION1BETA1_JSON);
    return this;
  }

}
