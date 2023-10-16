/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.shardeddbops;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpecScheduling;
import io.stackgres.testutil.fixture.Fixture;

public class ShardedDbOpsSchedulingFixture extends Fixture<StackGresShardedDbOpsSpecScheduling> {

  public ShardedDbOpsSchedulingFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_SCHEDULING_JSON);
    return this;
  }

}
