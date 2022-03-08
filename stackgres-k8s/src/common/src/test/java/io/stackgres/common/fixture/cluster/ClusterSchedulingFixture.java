/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.cluster;

import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.testutil.fixture.Fixture;

public class ClusterSchedulingFixture extends Fixture<StackGresClusterPodScheduling> {

  public ClusterSchedulingFixture loadDefault() {
    fixture = readFromJson(STACKGRES_CLUSTER_SCHEDULING_JSON);
    return this;
  }

  public ClusterSchedulingFixture withNodeAffinity(NodeAffinity nodeAffinity) {
    fixture.setNodeAffinity(nodeAffinity);
    return this;
  }

}
