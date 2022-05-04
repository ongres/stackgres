/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.testutil.JsonUtil;

public class StackGresPostgresConfigFixture {

  public StackGresPostgresConfig build(String jsonFilename) {
    return JsonUtil.readFromJson(format("postgres_config/%s.json", jsonFilename),
        StackGresPostgresConfig.class);
  }

}
