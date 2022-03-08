/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.cluster;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.testutil.fixture.Fixture;

public class JsonClusterFixture extends Fixture<ObjectNode> {

  public JsonClusterFixture loadFromVersion1() {
    fixture = readFromJsonAsJson(STACKGRES_CLUSTER_FROM_VERSION1_JSON);
    return this;
  }

  public JsonClusterFixture loadFromVersion1beta1() {
    fixture = readFromJsonAsJson(STACKGRES_CLUSTER_FROM_VERSION1BETA1_JSON);
    return this;
  }

  public JsonClusterFixture loadToVersion1() {
    fixture = readFromJsonAsJson(STACKGRES_CLUSTER_TO_VERSION1_JSON);
    return this;
  }

  public JsonClusterFixture loadToVersion1beta1() {
    fixture = readFromJsonAsJson(STACKGRES_CLUSTER_TO_VERSION1BETA1_JSON);
    return this;
  }

}
