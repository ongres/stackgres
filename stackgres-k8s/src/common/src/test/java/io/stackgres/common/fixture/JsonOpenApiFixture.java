/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonOpenApiFixture extends Fixture<ObjectNode> {

  public JsonOpenApiFixture loadDefault() {
    fixture = readFromJsonXzAsJson(OPENAPI_JSON_XZ);
    return this;
  }

}
