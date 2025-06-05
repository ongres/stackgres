/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ShardedDbOpsMajorVersionUpgradeJobTest extends ShardedDbOpsJobTestCase {

  @Override
  StackGresShardedDbOps getShardedDbOps() {
    return Fixtures.shardedDbOps().loadMajorVersionUpgrade().get();
  }

}
