/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.stackgres.common.ManagedSqlCronUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Keep track, in memory, of the executions of the SGScript entries that set the field
 * {@code cron}. After a restart each of those entries is executed once again, as soon as the
 * managed SQL is reconciled.
 */
@ApplicationScoped
public class ManagedSqlCronScheduler {

  private final Clock clock;
  private final Map<String, Instant> lastExecutions = new ConcurrentHashMap<>();
  private final Map<String, String> notifiedSchedules = new ConcurrentHashMap<>();
  private final AtomicReference<Instant> nextExecution = new AtomicReference<>();

  public ManagedSqlCronScheduler() {
    this(Clock.systemUTC());
  }

  ManagedSqlCronScheduler(Clock clock) {
    this.clock = clock;
  }

  public Instant now() {
    return clock.instant();
  }

  /**
   * Return {@code true} if the script entry was never executed (with its current version) or if a
   * scheduled execution time passed since its last execution.
   */
  public boolean isDue(
      StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry) {
    final Instant lastExecution = lastExecutions.get(key(managedScript, scriptEntry));
    if (lastExecution == null) {
      return true;
    }
    return ManagedSqlCronUtil.lastExecution(scriptEntry.getCron(), zonedNow())
        .map(ZonedDateTime::toInstant)
        .filter(lastScheduledExecution -> lastScheduledExecution.isAfter(lastExecution))
        .isPresent();
  }

  /**
   * Return {@code true}, only once since the start of this cluster-controller, the first time that
   * the script entry is executed with the specified cron expression and SQL, so that the schedule
   * of a script entry is notified again when any of them change.
   */
  public boolean isScheduleToNotify(
      StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry,
      String sql) {
    final String schedule = scriptEntry.getCron() + "\n" + sql;
    return !Objects.equals(
        notifiedSchedules.put(scheduleKey(managedScript, scriptEntry), schedule), schedule);
  }

  /**
   * Return a description of the cadence of the schedule of the script entry.
   */
  public String getCadence(StackGresScriptEntry scriptEntry) {
    return "cron " + scriptEntry.getCron()
        + ManagedSqlCronUtil.nextInterval(scriptEntry.getCron(), zonedNow())
        .map(interval -> ", every " + interval.toString().substring(2).toLowerCase(Locale.US))
        .orElse("");
  }

  public void executed(
      StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry) {
    lastExecutions.put(key(managedScript, scriptEntry), now());
  }

  /**
   * Set the next time at which any of the specified script entries has to be executed. An empty
   * collection (or a Pod that is not the primary) disables the scheduled executions.
   */
  public void schedule(Collection<StackGresScriptEntry> cronScriptEntries) {
    final ZonedDateTime now = zonedNow();
    nextExecution.set(cronScriptEntries.stream()
        .map(StackGresScriptEntry::getCron)
        .filter(Objects::nonNull)
        .map(cron -> ManagedSqlCronUtil.nextExecution(cron, now))
        .flatMap(Optional::stream)
        .map(ZonedDateTime::toInstant)
        .min(Instant::compareTo)
        .orElse(null));
  }

  public void unschedule() {
    nextExecution.set(null);
  }

  public boolean isAnyExecutionDue() {
    final Instant next = nextExecution.get();
    return next != null && !now().isBefore(next);
  }

  private ZonedDateTime zonedNow() {
    return now().atZone(ZoneOffset.UTC);
  }

  private String key(
      StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry) {
    return scheduleKey(managedScript, scriptEntry) + "/" + scriptEntry.getVersion();
  }

  private String scheduleKey(
      StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry) {
    return managedScript.getId() + "/" + managedScript.getSgScript()
        + "/" + scriptEntry.getId();
  }

}
