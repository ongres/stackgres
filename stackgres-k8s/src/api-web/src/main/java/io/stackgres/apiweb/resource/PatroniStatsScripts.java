/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public enum PatroniStatsScripts {

  CPU_FOUND("cpuFound"),
  CPU_QUOTA("cpuQuota"),
  CPU_PERIOD("cpuPeriod"),
  CPU_PSI_AVG10("cpuPsiAvg10"),
  CPU_PSI_AVG60("cpuPsiAvg60"),
  CPU_PSI_AVG300("cpuPsiAvg300"),
  CPU_PSI_TOTAL("cpuPsiTotal"),
  MEMORY_FOUND("memoryFound"),
  MEMORY_USED("memoryUsed"),
  MEMORY_PSI_AVG10("memoryPsiAvg10"),
  MEMORY_PSI_AVG60("memoryPsiAvg60"),
  MEMORY_PSI_AVG300("memoryPsiAvg300"),
  MEMORY_PSI_TOTAL("memoryPsiTotal"),
  MEMORY_PSI_FULL_AVG10("memoryPsiFullAvg10"),
  MEMORY_PSI_FULL_AVG60("memoryPsiFullAvg60"),
  MEMORY_PSI_FULL_AVG300("memoryPsiFullAvg300"),
  MEMORY_PSI_FULL_TOTAL("memoryPsiFullTotal"),
  DISK_FOUND("diskFound"),
  DISK_USED("diskUsed"),
  DISK_PSI_AVG10("diskPsiAvg10"),
  DISK_PSI_AVG60("diskPsiAvg60"),
  DISK_PSI_AVG300("diskPsiAvg300"),
  DISK_PSI_TOTAL("diskPsiTotal"),
  DISK_PSI_FULL_AVG10("diskPsiFullAvg10"),
  DISK_PSI_FULL_AVG60("diskPsiFullAvg60"),
  DISK_PSI_FULL_AVG300("diskPsiFullAvg300"),
  DISK_PSI_FULL_TOTAL("diskPsiFullTotal"),
  LOAD_1M("load1m"),
  LOAD_5M("load5m"),
  LOAD_10M("load10m"),
  CONNECTIONS("connections");

  private static final ImmutableMap<PatroniStatsScripts, String> SCRIPTS = readScripts();

  private final String name;

  PatroniStatsScripts(String name) {
    this.name = name;
  }

  private static ImmutableMap<PatroniStatsScripts, String> readScripts() {
    Properties properties = new Properties();
    try (InputStream is = PatroniStatsScripts.class.getResourceAsStream(
        "/patroni-stats.properties")) {
      properties.load(is);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return Seq.of(values())
        .map(script -> Tuple.tuple(script, properties.getProperty(script.name)))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
  }

  public String getName() {
    return name;
  }

  public static ImmutableMap<PatroniStatsScripts, String> getScripts() {
    return SCRIPTS;
  }

  public static PatroniStatsScripts fromName(String name) {
    return Seq.of(values())
        .filter(e -> e.name.equals(name))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException("No enum with name " + name));
  }

}
