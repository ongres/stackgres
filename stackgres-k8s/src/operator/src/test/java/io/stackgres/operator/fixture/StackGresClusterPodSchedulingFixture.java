/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;

public class StackGresClusterPodSchedulingFixture {

  private NodeAffinity nodeAffinity;

  public StackGresClusterPodSchedulingFixture withNodeAffinity(NodeAffinity nodeAffinity) {
    this.nodeAffinity = nodeAffinity;
    return this;
  }

  public StackGresClusterPodScheduling build() {
    StackGresClusterPodScheduling scheduling = new StackGresClusterPodScheduling();
    scheduling.setNodeAffinity(nodeAffinity);
    return scheduling;
  }

}
