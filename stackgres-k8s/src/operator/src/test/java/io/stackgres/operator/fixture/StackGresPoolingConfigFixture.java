/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.testutil.JsonUtil;

public class StackGresPoolingConfigFixture {

  public StackGresPoolingConfig build(String jsonFilename) {
    return JsonUtil.readFromJson(format("pooling_config/%s.json", jsonFilename),
        StackGresPoolingConfig.class);
  }

}
