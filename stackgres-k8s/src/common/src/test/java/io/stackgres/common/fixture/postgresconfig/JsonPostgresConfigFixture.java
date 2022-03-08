/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.postgresconfig;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonPostgresConfigFixture extends Fixture<ObjectNode> {

  public JsonPostgresConfigFixture loadFromVersion1() {
    fixture = readFromJsonAsJson(STACKGRES_POSTGRES_CONFIG_FROM_VERSION1_JSON);
    return this;
  }

  public JsonPostgresConfigFixture loadFromVersion1beta1() {
    fixture = readFromJsonAsJson(STACKGRES_POSTGRES_CONFIG_FROM_VERSION1BETA1_JSON);
    return this;
  }

  public JsonPostgresConfigFixture loadToVersion1() {
    fixture = readFromJsonAsJson(STACKGRES_POSTGRES_CONFIG_TO_VERSION1_JSON);
    return this;
  }

  public JsonPostgresConfigFixture loadToVersion1beta1() {
    fixture = readFromJsonAsJson(STACKGRES_POSTGRES_CONFIG_TO_VERSION1BETA1_JSON);
    return this;
  }

}
