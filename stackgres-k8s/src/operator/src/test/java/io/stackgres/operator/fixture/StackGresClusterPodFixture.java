/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;

public class StackGresClusterPodFixture {

  private StackGresClusterPodScheduling scheduling;

  public StackGresClusterPodFixture withScheduling(StackGresClusterPodScheduling scheduling) {
    this.scheduling = scheduling;
    return this;
  }

  public StackGresClusterPod build() {
    StackGresClusterPod pod = new StackGresClusterPod();
    pod.setScheduling(scheduling);
    return pod;
  }

}
