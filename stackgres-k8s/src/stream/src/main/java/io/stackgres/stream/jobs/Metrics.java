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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.tuple.Tuple;

@Singleton
public class Metrics {

  private static final String STREAM_METRIC_PREFIX = "stream_";

  private final MeterRegistry registry;
  private final Counter totalNumberOfEventsSentCounter;
  private final Counter totalNumberOfErrorsSeenConuter;
  private final Map<String, Number> attributes;
  private final Map<String, Gauge> attributesGauge;

  private Boolean lastEventWasSent = true;
  private String lastEventSent;
  private Long totalNumberOfEventsSent = 0L;
  private Long totalNumberOfErrorsSeen = 0L;
  private String lastErrorSeen;

  @Inject
  public Metrics(MeterRegistry registry) {
    this.registry = registry;
    Gauge
        .builder(STREAM_METRIC_PREFIX + "last_event_was_sent", this::getLastEventWasSentAsNumber)
        .register(registry);
    this.totalNumberOfEventsSentCounter = Counter
        .builder(STREAM_METRIC_PREFIX + "total_number_of_events_sent")
        .register(registry);
    this.totalNumberOfErrorsSeenConuter = Counter
        .builder(STREAM_METRIC_PREFIX + "total_number_of_errors_seen")
        .register(registry);
    this.attributes = new HashMap<>();
    this.attributesGauge = new HashMap<>();
  }

  public Boolean isLastEventWasSent() {
    return lastEventWasSent;
  }

  public Integer getLastEventWasSentAsNumber() {
    return lastEventWasSent ? 1 : 0;
  }

  public void setLastEventWasSent(boolean lastEventWasSent) {
    this.lastEventWasSent = lastEventWasSent;
  }

  public String getLastEventSent() {
    return lastEventSent;
  }

  public void setLastEventSent(String lastEventSent) {
    this.lastEventSent = lastEventSent;
  }

  public Long getTotalNumberOfEventsSent() {
    return totalNumberOfEventsSent;
  }

  public void incrementTotalNumberOfEventsSent(int size) {
    totalNumberOfEventsSent += size;
    totalNumberOfEventsSentCounter.increment(size);
  }

  public String getLastErrorSeen() {
    return lastErrorSeen;
  }

  public void setLastErrorSeen(String lastErrorSeen) {
    this.lastErrorSeen = lastErrorSeen;
  }

  public Long getTotalNumberOfErrorsSeen() {
    return totalNumberOfErrorsSeen;
  }

  public void incrementTotalNumberOfErrorsSeen() {
    totalNumberOfErrorsSeen++;
    totalNumberOfErrorsSeenConuter.increment();
  }

  public void gauge(String attributeName, Number attributeValueNumber) {
    String attributeNameNormalized = Pattern.compile(".")
        .matcher(attributeName)
        .results()
        .map(result -> Tuple.tuple(result.group(), result.group().toLowerCase(Locale.US)))
        .map(t -> t.v1.equals(t.v2) ? t.v1 : "_" + t.v2)
        .collect(Collectors.joining())
        .replaceAll("^_", "");
    attributes.put(attributeNameNormalized, attributeValueNumber);
    attributesGauge.computeIfAbsent(
        attributeNameNormalized,
        key -> Gauge
            .builder(
                attributeNameNormalized,
                () -> attributes.get(attributeNameNormalized))
            .register(registry));
  }

}
