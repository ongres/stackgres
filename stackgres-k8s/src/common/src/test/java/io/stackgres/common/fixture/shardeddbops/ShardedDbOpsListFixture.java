/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.shardeddbops;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsList;
import io.stackgres.testutil.fixture.Fixture;

public class ShardedDbOpsListFixture extends Fixture<StackGresShardedDbOpsList> {

  public ShardedDbOpsListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_LIST_JSON);
    return this;
  }

  public ShardedDbOpsListFixture withJustFirstElement() {
    if (fixture.getItems() != null && !fixture.getItems().isEmpty()) {
      fixture.setItems(fixture.getItems().subList(0, 1));
    }
    return this;
  }

}
