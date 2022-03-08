/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.upgrade;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonPatroniConfigFixture extends Fixture<ObjectNode> {

  public JsonPatroniConfigFixture loadV1_0() {
    fixture = readFromJsonAsJson(UPGRADE_V1_0_PATRONI_JSON);
    return this;
  }

  public JsonPatroniConfigFixture loadV1_1() {
    fixture = readFromJsonAsJson(UPGRADE_V1_1_PATRONI_JSON);
    return this;
  }

}
