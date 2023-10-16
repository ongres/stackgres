/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.dbops;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;
import io.stackgres.testutil.fixture.Fixture;

public class DbOpsListFixture extends Fixture<StackGresDbOpsList> {

  public DbOpsListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_DB_OPS_LIST_JSON);
    return this;
  }

  public DbOpsListFixture withJustFirstElement() {
    if (fixture.getItems() != null && !fixture.getItems().isEmpty()) {
      fixture.setItems(fixture.getItems().subList(0, 1));
    }
    return this;
  }

}
