/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import io.micrometer.core.instrument.MeterRegistry;
import io.stackgres.common.metrics.AbstractMetrics;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class Metrics extends AbstractMetrics {

  private Boolean lastEventWasSent;
  private String lastEventSent;
  private long totalNumberOfEventsSent = 0L;
  private String lastErrorSeen;
  private long totalNumberOfErrorsSeen = 0L;

  @Inject
  public Metrics(MeterRegistry registry) {
    super(registry, "stream");
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
        prefix + "last_event_was_sent",
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
        prefix + "total_number_of_events_sent",
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
        prefix + "total_number_of_errors_seen",
        this,
        Metrics::getTotalNumberOfErrorsSeen);
  }

}
