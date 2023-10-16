/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;

@QuarkusTest
class ShardedDbOpsMinorVersionUpgradeJobTest extends ShardedDbOpsJobTestCase {

  @Override
  StackGresShardedDbOps getShardedDbOps() {
    return Fixtures.shardedDbOps().loadMinorVersionUpgrade().get();
  }

}
