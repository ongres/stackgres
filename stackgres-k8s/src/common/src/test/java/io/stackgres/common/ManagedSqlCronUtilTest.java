/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class ManagedSqlCronUtilTest {

  @Test
  void everyInterval_shouldUseSecondsBelowAMinute() {
    assertEquals("0/10 * * * * ?", ManagedSqlCronUtil.everyInterval(Duration.ofSeconds(10)));
    assertEquals("0/1 * * * * ?", ManagedSqlCronUtil.everyInterval(Duration.ofMillis(100)));
  }

  @Test
  void everyInterval_shouldUseMinutesBelowAnHour() {
    assertEquals("0 0/1 * * * ?", ManagedSqlCronUtil.everyInterval(Duration.ofSeconds(90)));
    assertEquals("0 0/15 * * * ?", ManagedSqlCronUtil.everyInterval(Duration.ofMinutes(15)));
  }

  @Test
  void everyInterval_shouldUseHoursUpToADay() {
    assertEquals("0 0 0/2 * * ?", ManagedSqlCronUtil.everyInterval(Duration.ofHours(2)));
    assertEquals("0 0 0/23 * * ?", ManagedSqlCronUtil.everyInterval(Duration.ofDays(3)));
  }

  @Test
  void everyInterval_shouldReturnValidExpressions() {
    for (Duration interval : new Duration[] {
        Duration.ofSeconds(1), Duration.ofSeconds(59), Duration.ofMinutes(59), Duration.ofDays(1)}) {
      assertTrue(ManagedSqlCronUtil.isValid(ManagedSqlCronUtil.everyInterval(interval)),
          interval.toString());
    }
  }

  @Test
  void isValid_shouldRejectInvalidExpressions() {
    assertFalse(ManagedSqlCronUtil.isValid("not a cron"));
    assertFalse(ManagedSqlCronUtil.isValid("*/5 * * * *"));
  }

  @Test
  void nextInterval_shouldReturnTheCadenceOfTheSchedule() {
    ZonedDateTime now = ZonedDateTime.of(2026, 9, 28, 10, 0, 15, 0, ZoneOffset.UTC);
    assertEquals(Duration.ofSeconds(10),
        ManagedSqlCronUtil.nextInterval("0/10 * * * * ?", now).orElseThrow());
    assertEquals(Duration.ofMinutes(2),
        ManagedSqlCronUtil.nextInterval("0 0/2 * * * ?", now).orElseThrow());
  }

  @Test
  void lastAndNextExecution_shouldFollowTheSchedule() {
    ZonedDateTime now = ZonedDateTime.of(2026, 9, 28, 10, 0, 15, 0, ZoneOffset.UTC);
    assertEquals(now.withSecond(10),
        ManagedSqlCronUtil.lastExecution("0/10 * * * * ?", now).orElseThrow());
    assertEquals(now.withSecond(20),
        ManagedSqlCronUtil.nextExecution("0/10 * * * * ?", now).orElseThrow());
  }

}
