/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.upgrade;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.testutil.fixture.Fixture;

public class ClusterFixture extends Fixture<StackGresCluster> {

  public ClusterFixture loadDefault() {
    fixture = readFromJson(UPGRADE_SGCLUSTER_JSON);
    return this;
  }

}
