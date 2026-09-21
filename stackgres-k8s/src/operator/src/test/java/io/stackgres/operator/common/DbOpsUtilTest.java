/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import org.junit.jupiter.api.Test;

class DbOpsUtilTest {

  @Test
  void givenNoStatusUpdateDelay_shouldReturnTheDefaultStatusUpdateDelay() {
    assertEquals(
        Duration.ofMinutes(1),
        DbOpsUtil.getStatusUpdateDelay(dbOpsWithSpec(new StackGresDbOpsSpec())));
  }

  @Test
  void givenARestartStatusUpdateDelay_shouldReturnIt() {
    var spec = new StackGresDbOpsSpec();
    spec.setRestart(new StackGresDbOpsRestart());
    spec.getRestart().setStatusUpdateDelay("PT30S");

    assertEquals(Duration.ofSeconds(30), DbOpsUtil.getStatusUpdateDelay(dbOpsWithSpec(spec)));
  }

  @Test
  void givenASecurityUpgradeStatusUpdateDelay_shouldReturnIt() {
    var spec = new StackGresDbOpsSpec();
    spec.setSecurityUpgrade(new StackGresDbOpsSecurityUpgrade());
    spec.getSecurityUpgrade().setStatusUpdateDelay("PT2M");

    assertEquals(Duration.ofMinutes(2), DbOpsUtil.getStatusUpdateDelay(dbOpsWithSpec(spec)));
  }

  @Test
  void givenAMinorVersionUpgradeStatusUpdateDelay_shouldReturnIt() {
    var spec = new StackGresDbOpsSpec();
    spec.setMinorVersionUpgrade(new StackGresDbOpsMinorVersionUpgrade());
    spec.getMinorVersionUpgrade().setStatusUpdateDelay("PT1H");

    assertEquals(Duration.ofHours(1), DbOpsUtil.getStatusUpdateDelay(dbOpsWithSpec(spec)));
  }

  @Test
  void givenAnInvalidStatusUpdateDelay_shouldReturnTheDefaultStatusUpdateDelay() {
    var spec = new StackGresDbOpsSpec();
    spec.setRestart(new StackGresDbOpsRestart());
    spec.getRestart().setStatusUpdateDelay("1 minute");

    assertEquals(Duration.ofMinutes(1), DbOpsUtil.getStatusUpdateDelay(dbOpsWithSpec(spec)));
  }

  private StackGresDbOps dbOpsWithSpec(StackGresDbOpsSpec spec) {
    var dbOps = new StackGresDbOps();
    dbOps.setSpec(spec);
    return dbOps;
  }

}
