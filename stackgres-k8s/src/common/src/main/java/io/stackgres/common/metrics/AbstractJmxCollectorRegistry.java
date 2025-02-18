/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.metrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.management.MalformedObjectNameException;

import io.prometheus.jmx.JmxCollector;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.stackgres.common.CdiUtil;

public abstract class AbstractJmxCollectorRegistry {
  private final PrometheusRegistry collectorRegistry;
  private final PrometheusTextFormatWriter textFormatter = new PrometheusTextFormatWriter(true);

  public AbstractJmxCollectorRegistry(String yamlConfig) throws MalformedObjectNameException {
    new JmxCollector(yamlConfig).register(PrometheusRegistry.defaultRegistry);
    this.collectorRegistry = PrometheusRegistry.defaultRegistry;
  }

  public AbstractJmxCollectorRegistry() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.collectorRegistry = null;
  }

  public String scrape(String contentType) {
    try (
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
      textFormatter.write(stream, collectorRegistry.scrape());
      return stream.toString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
