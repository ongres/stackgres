/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;

public class StackGresClusterFixture {
  public static final String POSTGRES_LATEST_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().get(0).get();
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
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPods(new StackGresClusterPods());
    cluster.getSpec().getPods().setScheduling(new StackGresClusterPodsScheduling());
    cluster.getSpec().getPods().getScheduling().setNodeAffinity(nodeAffinity);
    return cluster;
  }

}
