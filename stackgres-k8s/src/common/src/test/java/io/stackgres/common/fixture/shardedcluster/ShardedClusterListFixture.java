/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.shardedcluster;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterList;
import io.stackgres.testutil.fixture.Fixture;

public class ShardedClusterListFixture extends Fixture<StackGresShardedClusterList> {

  public ShardedClusterListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_CLUSTER_LIST_JSON);
    return this;
  }

}
