/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.configuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.management.MalformedObjectNameException;

import io.prometheus.jmx.JmxCollector;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.stackgres.stream.app.StreamProperty;
import jakarta.inject.Singleton;

@Singleton
public class JmxCollectorRegistry {
  private final PrometheusRegistry collectorRegistry;
  private final PrometheusTextFormatWriter textFormatter = new PrometheusTextFormatWriter(true);

  public JmxCollectorRegistry() throws MalformedObjectNameException {
    new JmxCollector(StreamProperty.STREAM_JMX_COLLECTOR_YAML_CONFIG.get().orElse("")).register();
    collectorRegistry = PrometheusRegistry.defaultRegistry;
  }

  public String scrape() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      textFormatter.write(stream, collectorRegistry.scrape());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return stream.toString(StandardCharsets.UTF_8);
  }
}
