/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsBuilder;

public class StackGresClusterPodFixture {

  private StackGresClusterPodScheduling scheduling;

  public StackGresClusterPodFixture withScheduling(StackGresClusterPodScheduling scheduling) {
    this.scheduling = scheduling;
    return this;
  }

  public StackGresClusterPods build() {
    StackGresClusterPods pod = new StackGresClusterPods();
    pod.setScheduling(scheduling);
    return pod;
  }

  public StackGresClusterPodsBuilder getBuilder() {
    return new StackGresClusterPodsBuilder(build());
  }

}
