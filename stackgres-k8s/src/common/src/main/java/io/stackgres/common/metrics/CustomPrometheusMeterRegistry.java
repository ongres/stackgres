/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.metrics;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.inject.Singleton;

@Singleton
public class CustomPrometheusMeterRegistry extends PrometheusMeterRegistry {

  private final AbstractJmxCollectorRegistry jmxCollectorRegistry;

  public CustomPrometheusMeterRegistry(
      PrometheusConfig config,
      AbstractJmxCollectorRegistry jmxCollectorRegistry) {
    super(config);
    this.jmxCollectorRegistry = jmxCollectorRegistry;
  }

  @Override
  public String scrape(String contentType) {
    StringBuilder sb = new StringBuilder();
    sb.append(super.scrape(contentType));
    if (jmxCollectorRegistry != null) {
      sb.append(jmxCollectorRegistry.scrape(contentType));
    }
    return sb.toString();
  }
}
