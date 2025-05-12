/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.metrics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import io.stackgres.common.CdiUtil;
import org.jooq.lambda.tuple.Tuple;

public abstract class AbstractMetrics {

  protected final MeterRegistry registry;
  protected final String prefix;
  protected Map<String, Number> gauges = new HashMap<>();

  public AbstractMetrics(
      MeterRegistry registry,
      String prefix) {
    this.registry = registry;
    this.prefix = "sg_" + prefix + "_";
  }

  public AbstractMetrics() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.registry = null;
    this.prefix = null;
  }

  public String getPrefix() {
    return prefix;
  }

  public void gauge(String attributeName, Number attributeValueNumber) {
    String attributeNameNormalized = Pattern.compile(".")
        .matcher(attributeName)
        .results()
        .map(result -> Tuple.tuple(result.group(), result.group().toLowerCase(Locale.US)))
        .map(t -> t.v1.equals(t.v2) ? t.v1 : "_" + t.v2)
        .collect(Collectors.joining())
        .replaceAll("^_", "");
    final String name = prefix + attributeNameNormalized;
    gauges.put(name, attributeValueNumber);
    registry.gauge(name, this, metrics -> metrics.getGauge(name));
  }

  public double getGauge(String key) {
    Number gauge = this.gauges.get(key);
    if (gauge == null) {
      return Double.NaN;
    }
    return gauge.doubleValue();
  }

}
