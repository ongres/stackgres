/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

/**
 * Schedule of the SGScript entries that set the field {@code cron}. The Quartz definition is used
 * since it is the one that allows to specify the seconds.
 */
public interface ManagedSqlCronUtil {

  CronParser CRON_PARSER =
      new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

  static boolean isValid(String cron) {
    try {
      CRON_PARSER.parse(cron).validate();
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  static Optional<ZonedDateTime> lastExecution(String cron, ZonedDateTime now) {
    return ExecutionTime.forCron(CRON_PARSER.parse(cron)).lastExecution(now);
  }

  static Optional<ZonedDateTime> nextExecution(String cron, ZonedDateTime now) {
    return ExecutionTime.forCron(CRON_PARSER.parse(cron)).nextExecution(now);
  }

  /**
   * Return the interval between the next two executions of the schedule.
   */
  static Optional<Duration> nextInterval(String cron, ZonedDateTime now) {
    return nextExecution(cron, now)
        .flatMap(next -> nextExecution(cron, next)
            .map(nextToNext -> Duration.between(next, nextToNext)));
  }

  /**
   * Return the Quartz cron expression that runs every specified interval. The interval is
   * truncated to seconds if lower than a minute, to minutes if lower than an hour and to hours
   * otherwise (up to 23 hours). An interval that does not divide its unit restarts at the
   * beginning of each minute, hour or day.
   */
  static String everyInterval(Duration interval) {
    final long seconds = Math.max(1, interval.toSeconds());
    if (seconds < 60) {
      return "0/" + seconds + " * * * * ?";
    }
    if (seconds < 3600) {
      return "0 0/" + (seconds / 60) + " * * * ?";
    }
    return "0 0 0/" + Math.min(23, seconds / 3600) + " * * ?";
  }

}
