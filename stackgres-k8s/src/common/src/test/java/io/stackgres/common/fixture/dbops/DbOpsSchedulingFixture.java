/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.dbops;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpecScheduling;
import io.stackgres.testutil.fixture.Fixture;

public class DbOpsSchedulingFixture extends Fixture<StackGresDbOpsSpecScheduling> {

  public DbOpsSchedulingFixture loadDefault() {
    fixture = readFromJson(STACKGRES_DB_OPS_SCHEDULING_JSON);
    return this;
  }

}
