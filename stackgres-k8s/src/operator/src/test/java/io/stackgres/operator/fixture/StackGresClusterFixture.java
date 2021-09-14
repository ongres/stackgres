/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.testutil.JsonUtil;

public class StackGresClusterFixture {

  private NodeAffinity nodeAffinity;

  public StackGresClusterFixture withNodeAffinity(NodeAffinity nodeAffinity) {
    this.nodeAffinity = nodeAffinity;
    return this;
  }

  public StackGresCluster empty() {
    return new StackGresCluster();
  }

  public StackGresCluster build() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpecFixture()
        .withPod(new StackGresClusterPodFixture()
            .withScheduling(new StackGresClusterPodSchedulingFixture()
                .withNodeAffinity(nodeAffinity)
                .build())
            .build())
        .build());
    return cluster;
  }

  public StackGresCluster build(String jsonName) {
    return JsonUtil.readFromJson(format("stackgres_cluster/%s.json", jsonName),
        StackGresCluster.class);
  }

}
