/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonSsaSanitizationFixture extends Fixture<ObjectNode> {

  public JsonSsaSanitizationFixture load(String resource) {
    fixture = readFromJsonAsJson("ssa-sanitization/" + resource);
    return this;
  }

}
