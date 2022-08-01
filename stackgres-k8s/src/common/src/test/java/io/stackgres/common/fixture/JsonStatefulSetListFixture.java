/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonStatefulSetListFixture extends Fixture<ObjectNode> {

  public JsonStatefulSetListFixture loadList() {
    fixture = readFromJsonAsJson(STATEFULSET_K8S_STS_LIST_RESPONSE_JSON);
    return this;
  }

}
