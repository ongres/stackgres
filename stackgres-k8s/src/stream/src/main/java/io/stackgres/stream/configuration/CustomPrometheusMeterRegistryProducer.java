/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.configuration;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CustomPrometheusMeterRegistryProducer {

  @Inject
  JmxCollectorRegistry jmxCollectorRegistry;

  @Produces
  @Singleton
  public PrometheusMeterRegistry createPrometheusMeterRegistry() {
    return new CustomPrometheusMeterRegistry(PrometheusConfig.DEFAULT, jmxCollectorRegistry);
  }

}
