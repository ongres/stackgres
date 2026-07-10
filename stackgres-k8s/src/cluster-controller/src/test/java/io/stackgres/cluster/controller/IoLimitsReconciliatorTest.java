/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolumeIoLimits;
import org.junit.jupiter.api.Test;

class IoLimitsReconciliatorTest {

  private StackGresClusterPodsPersistentVolumeIoLimits buildIoLimits(
      Integer readIops, Integer writeIops, Integer readMiBps, Integer writeMiBps) {
    var ioLimits = new StackGresClusterPodsPersistentVolumeIoLimits();
    ioLimits.setReadIops(readIops);
    ioLimits.setWriteIops(writeIops);
    ioLimits.setReadMiBps(readMiBps);
    ioLimits.setWriteMiBps(writeMiBps);
    return ioLimits;
  }

  @Test
  void mergeMostRestrictive_shouldReturnTheOtherWhenOneIsNull() {
    var ioLimits = buildIoLimits(1000, 2000, 100, 200);
    assertSame(ioLimits, IoLimitsReconciliator.mergeMostRestrictive(ioLimits, null));
    assertSame(ioLimits, IoLimitsReconciliator.mergeMostRestrictive(null, ioLimits));
    assertNull(IoLimitsReconciliator.mergeMostRestrictive(null, null));
  }

  @Test
  void mergeMostRestrictive_shouldTakeTheMinimumOfEachLimit() {
    var merged = IoLimitsReconciliator.mergeMostRestrictive(
        buildIoLimits(1000, null, 100, 400),
        buildIoLimits(2000, 3000, null, 200));
    assertEquals(1000, merged.getReadIops());
    assertEquals(3000, merged.getWriteIops());
    assertEquals(100, merged.getReadMiBps());
    assertEquals(200, merged.getWriteMiBps());
  }

}
