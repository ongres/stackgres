/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManagedSqlCronSchedulerTest {

  private ManagedSqlCronScheduler cronScheduler;

  private StackGresClusterManagedScriptEntry managedScript;

  private StackGresScriptEntry scriptEntry;

  @BeforeEach
  void setUp() {
    cronScheduler = new ManagedSqlCronScheduler(
        Clock.fixed(Instant.parse("2026-10-02T10:00:03Z"), ZoneOffset.UTC));
    managedScript = new StackGresClusterManagedScriptEntry();
    managedScript.setId(1);
    managedScript.setSgScript("test");
    scriptEntry = new StackGresScriptEntry();
    scriptEntry.setId(2);
    scriptEntry.setVersion(0);
    scriptEntry.setCron("0/10 * * * * ?");
  }

  @Test
  void isScheduleToNotify_shouldOnlyReturnTrueTheFirstTime() {
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
    assertFalse(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
  }

  @Test
  void isScheduleToNotify_whenTheVersionChanges_shouldNotReturnTrueAgain() {
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
    scriptEntry.setVersion(1);
    assertFalse(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
  }

  @Test
  void isScheduleToNotify_whenTheCronChanges_shouldReturnTrueAgain() {
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
    scriptEntry.setCron("0/20 * * * * ?");
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
    assertFalse(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
  }

  @Test
  void isScheduleToNotify_whenTheScriptChanges_shouldReturnTrueAgain() {
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 2"));
    assertFalse(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 2"));
  }

  @Test
  void isScheduleToNotify_shouldBeTrackedForEachScriptEntry() {
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, scriptEntry, "SELECT 1"));
    StackGresScriptEntry otherScriptEntry = new StackGresScriptEntry();
    otherScriptEntry.setId(3);
    otherScriptEntry.setCron(scriptEntry.getCron());
    assertTrue(cronScheduler.isScheduleToNotify(managedScript, otherScriptEntry, "SELECT 1"));
  }

  @Test
  void getCadence_shouldIncludeTheCronExpressionAndTheInterval() {
    assertEquals("cron 0/10 * * * * ?, every 10s", cronScheduler.getCadence(scriptEntry));
  }

}
