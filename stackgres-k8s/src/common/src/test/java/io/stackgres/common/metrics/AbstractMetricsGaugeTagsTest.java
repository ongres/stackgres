/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class AbstractMetricsGaugeTagsTest {

  static class TestMetrics extends AbstractMetrics {
    TestMetrics(MeterRegistry registry) {
      super(registry, "test");
    }

    void gauge(String name, String resource, double value) {
      registryGauge(name, List.of(new ImmutableTag("resource", resource)), this,
          metrics -> value);
    }
  }

  @Test
  void registryGauge_shouldRegisterTheSameGaugeForEachTagSet() {
    MeterRegistry registry = new SimpleMeterRegistry();
    TestMetrics metrics = new TestMetrics(registry);

    metrics.gauge("reconciliation_total_performed", "a", 1);
    metrics.gauge("reconciliation_total_performed", "b", 2);
    metrics.gauge("reconciliation_total_performed", "b", 3);

    assertEquals(2, registry.find("sg_test_reconciliation_total_performed").gauges().size());
    assertEquals(1, registry.get("sg_test_reconciliation_total_performed")
        .tag("resource", "a").gauge().value());
    assertEquals(2, registry.get("sg_test_reconciliation_total_performed")
        .tag("resource", "b").gauge().value());
  }

}
