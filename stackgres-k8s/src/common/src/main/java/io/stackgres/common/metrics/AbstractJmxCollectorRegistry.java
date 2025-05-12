/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.metrics;

import javax.management.MalformedObjectNameException;

import io.prometheus.jmx.JmxCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.stackgres.common.CdiUtil;

public abstract class AbstractJmxCollectorRegistry {

  private final PrometheusRegistry collectorRegistry;

  public AbstractJmxCollectorRegistry(String yamlConfig) throws MalformedObjectNameException {
    new JmxCollector(yamlConfig).register(PrometheusRegistry.defaultRegistry);
    this.collectorRegistry = PrometheusRegistry.defaultRegistry;
  }

  public AbstractJmxCollectorRegistry() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.collectorRegistry = null;
  }

  public MetricSnapshots scrape() {
    return collectorRegistry.scrape();
  }

}
