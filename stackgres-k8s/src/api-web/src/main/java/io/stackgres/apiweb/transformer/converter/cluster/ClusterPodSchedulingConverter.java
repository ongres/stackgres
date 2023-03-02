/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.converter.cluster;

import io.stackgres.apiweb.dto.cluster.ClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;

public class ClusterPodSchedulingConverter {

  public ClusterPodScheduling from(StackGresClusterPodScheduling source) {
    ClusterPodScheduling podScheduling = new ClusterPodScheduling();
    podScheduling.setNodeSelector(source.getNodeSelector());
    podScheduling.setTolerations(source.getTolerations());
    podScheduling.setNodeAffinity(source.getNodeAffinity());
    podScheduling.setPriorityClassName(source.getPriorityClassName());
    return podScheduling;
  }

  public StackGresClusterPodScheduling to(ClusterPodScheduling sourceScheduling) {
    StackGresClusterPodScheduling targetScheduling = new StackGresClusterPodScheduling();
    targetScheduling.setNodeSelector(sourceScheduling.getNodeSelector());
    targetScheduling.setTolerations(sourceScheduling.getTolerations());
    targetScheduling.setNodeAffinity(sourceScheduling.getNodeAffinity());
    targetScheduling.setPriorityClassName(sourceScheduling.getPriorityClassName());
    return targetScheduling;
  }
}
