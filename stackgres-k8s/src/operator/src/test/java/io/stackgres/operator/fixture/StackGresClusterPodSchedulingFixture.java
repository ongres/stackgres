/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.testutil.JsonUtil;

public class StackGresClusterPodSchedulingFixture {

  private NodeAffinity nodeAffinity;
  private StackGresClusterPodScheduling scheduling;

  public StackGresClusterPodSchedulingFixture withNodeAffinity(NodeAffinity nodeAffinity) {
    this.nodeAffinity = nodeAffinity;
    return this;
  }

  public StackGresClusterPodScheduling build() {
    buildPodNodeAffinityScheduling();
    return scheduling;
  }

  public StackGresClusterPodSchedulingFixture buildPodNodeAffinityScheduling() {
    scheduling = new StackGresClusterPodScheduling();
    if (nodeAffinity != null) {
      scheduling.setNodeAffinity(nodeAffinity);
    }
    return this;
  }

  public StackGresClusterPodScheduling loadPodNodeAffinityScheduling() {
    scheduling = JsonUtil
        .readFromJson("stackgres_cluster/scheduling.json",
            StackGresClusterPodScheduling.class);
    return scheduling;
  }

}
