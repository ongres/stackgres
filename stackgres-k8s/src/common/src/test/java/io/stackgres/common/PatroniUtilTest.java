/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterUpdateStrategy;
import org.junit.jupiter.api.Test;

class PatroniUtilTest {

  @Test
  void givenNoUpdateStrategy_shouldReturnTheDefaultRestartDelay() {
    assertEquals(
        Duration.ofMinutes(5),
        PatroniUtil.getRestartDelay(new StackGresCluster()));
  }

  @Test
  void givenARestartDelay_shouldReturnIt() {
    assertEquals(
        Duration.ofHours(1),
        PatroniUtil.getRestartDelay(clusterWithRestartDelay("PT1H")));
  }

  @Test
  void givenAnInvalidRestartDelay_shouldReturnTheDefaultRestartDelay() {
    assertEquals(
        Duration.ofMinutes(5),
        PatroniUtil.getRestartDelay(clusterWithRestartDelay("5 minutes")));
  }

  private StackGresCluster clusterWithRestartDelay(String restartDelay) {
    var cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPods(new StackGresClusterPods());
    cluster.getSpec().getPods().setUpdateStrategy(new StackGresClusterUpdateStrategy());
    cluster.getSpec().getPods().getUpdateStrategy().setRestartDelay(restartDelay);
    return cluster;
  }

}
