/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.configuration;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class CustomPrometheusMeterRegistryProducer extends PrometheusMeterRegistry {

  private final JmxCollectorRegistry jmxCollectorRegistry;

  public CustomPrometheusMeterRegistryProducer(PrometheusConfig config, JmxCollectorRegistry jmxCollectorRegistry) {
    super(config);
    this.jmxCollectorRegistry = jmxCollectorRegistry;
  }

  @Produces
  @Singleton
  public PrometheusMeterRegistry createPrometheusMeterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  @Override
  public String scrape() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.scrape());
    if (jmxCollectorRegistry != null) {
      sb.append(jmxCollectorRegistry.scrape());
    }

    return sb.toString();
  }
}
