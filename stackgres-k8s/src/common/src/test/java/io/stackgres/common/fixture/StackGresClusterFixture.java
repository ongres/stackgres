/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
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
    cluster.getSpec().setPod(new StackGresClusterPod());
    cluster.getSpec().getPod().setScheduling(new StackGresClusterPodScheduling());
    cluster.getSpec().getPod().getScheduling().setNodeAffinity(nodeAffinity);
    return cluster;
  }

}
