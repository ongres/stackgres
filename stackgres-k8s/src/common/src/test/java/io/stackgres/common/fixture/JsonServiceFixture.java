/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonServiceFixture extends Fixture<ObjectNode> {

  public JsonServiceFixture loadPrimaryServiceWithManagedFields() {
    fixture = readFromJsonAsJson(SERVICE_PRIMARY_SERVICE_WITH_MANAGED_FIELDS_JSON);
    return this;
  }

  public JsonServiceFixture loadPrimaryService() {
    fixture = readFromJsonAsJson(SERVICE_PRIMARY_SERVICE_JSON);
    return this;
  }

}
