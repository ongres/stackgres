/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.stream;

import io.stackgres.common.crd.sgstream.StackGresStreamList;
import io.stackgres.testutil.fixture.Fixture;

public class StreamListFixture extends Fixture<StackGresStreamList> {

  public StreamListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_CLUSTER_LIST_JSON);
    return this;
  }

  public StreamListFixture withJustFirstElement() {
    if (fixture.getItems() != null && !fixture.getItems().isEmpty()) {
      fixture.setItems(fixture.getItems().subList(0, 1));
    }
    return this;
  }

}
