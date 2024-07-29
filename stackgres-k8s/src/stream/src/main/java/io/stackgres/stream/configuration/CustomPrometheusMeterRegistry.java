/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exemplars.ExemplarSampler;

public class CustomPrometheusMeterRegistry extends PrometheusMeterRegistry {

  private final JmxCollectorRegistry jmxCollectorRegistry;

  public CustomPrometheusMeterRegistry(
      PrometheusConfig config,
      CollectorRegistry registry,
      Clock clock,
      ExemplarSampler exemplarSampler,
      JmxCollectorRegistry jmxCollectorRegistry) {
    super(config, registry, clock, exemplarSampler);
    this.jmxCollectorRegistry = jmxCollectorRegistry;
  }

  public CustomPrometheusMeterRegistry(
      PrometheusConfig config,
      CollectorRegistry registry,
      Clock clock,
      JmxCollectorRegistry jmxCollectorRegistry) {
    super(config, registry, clock);
    this.jmxCollectorRegistry = jmxCollectorRegistry;
  }

  public CustomPrometheusMeterRegistry(
      PrometheusConfig config,
      JmxCollectorRegistry jmxCollectorRegistry) {
    super(config);
    this.jmxCollectorRegistry = jmxCollectorRegistry;
  }

  @Override
  public String scrape(String contentType) {
    StringBuilder sb = new StringBuilder();
    sb.append(super.scrape(contentType));
    if (jmxCollectorRegistry != null) {
      sb.append(jmxCollectorRegistry.scrape());
    }
    return sb.toString();
  }
}
