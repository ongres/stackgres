/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.tuple.Tuple;

@Singleton
public class Metrics {

  private static final String STREAM_METRIC_PREFIX = "stream_";

  private final MeterRegistry registry;

  private Boolean lastEventWasSent;
  private String lastEventSent;
  private long totalNumberOfEventsSent = 0L;
  private String lastErrorSeen;
  private long totalNumberOfErrorsSeen = 0L;
  private Map<String, Number> gauges = new HashMap<>();

  @Inject
  public Metrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public Boolean isLastEventWasSent() {
    return lastEventWasSent;
  }

  public double isLastEventWasSentAsDouble() {
    return lastEventWasSent == null || lastEventWasSent ? 1 : 0;
  }

  public void setLastEventWasSent(Boolean lastEventWasSent) {
    this.lastEventWasSent = lastEventWasSent;
    registry.gauge(
        STREAM_METRIC_PREFIX + "last_event_was_sent",
        this,
        Metrics::isLastEventWasSentAsDouble);
  }

  public String getLastEventSent() {
    return lastEventSent;
  }

  public void setLastEventSent(String lastEventSent) {
    this.lastEventSent = lastEventSent;
  }

  public long getTotalNumberOfEventsSent() {
    return totalNumberOfEventsSent;
  }

  public void incrementTotalNumberOfEventsSent(int size) {
    totalNumberOfEventsSent = totalNumberOfEventsSent + size;
    registry.gauge(
        STREAM_METRIC_PREFIX + "total_number_of_events_sent",
        this,
        Metrics::getTotalNumberOfEventsSent);
  }

  public String getLastErrorSeen() {
    return lastErrorSeen;
  }

  public void setLastErrorSeen(String lastErrorSeen) {
    this.lastErrorSeen = lastErrorSeen;
  }

  public long getTotalNumberOfErrorsSeen() {
    return totalNumberOfErrorsSeen;
  }

  public void incrementTotalNumberOfErrorsSeen() {
    totalNumberOfErrorsSeen = totalNumberOfErrorsSeen + 1;
    registry.gauge(
        STREAM_METRIC_PREFIX + "total_number_of_errors_seen",
        this,
        Metrics::getTotalNumberOfErrorsSeen);
  }

  public void gauge(String attributeName, Number attributeValueNumber) {
    String attributeNameNormalized = Pattern.compile(".")
        .matcher(attributeName)
        .results()
        .map(result -> Tuple.tuple(result.group(), result.group().toLowerCase(Locale.US)))
        .map(t -> t.v1.equals(t.v2) ? t.v1 : "_" + t.v2)
        .collect(Collectors.joining())
        .replaceAll("^_", "");
    final String name = STREAM_METRIC_PREFIX + attributeNameNormalized;
    this.gauges.put(name, attributeValueNumber);
    registry.gauge(
        name,
        this,
        metrics -> getGauge(name));
  }

  public double getGauge(String key) {
    Number gauge = this.gauges.get(key);
    if (gauge == null) {
      return Double.NaN;
    }
    return gauge.doubleValue();
  }
}
