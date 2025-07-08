/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.stackgres.common.CdiUtil;
import org.jooq.lambda.tuple.Tuple;

public abstract class AbstractMetrics {

  private final MeterRegistry registry;
  private final String prefix;
  private Map<String, Number> gauges = new HashMap<>();
  private Set<String> registered = new HashSet<>();

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
    registryGauge(name, this, metrics -> metrics.getGauge(name));
  }

  protected <T> void registryGauge(
      String name,
      final T stateObject,
      final ToDoubleFunction<T> valueFunction) {
    name = prefix + name;
    if (!registered.contains(name)) {
      registry.gauge(name, stateObject, valueFunction);
      registered.add(name);
    }
  }

  protected <T> void registryGauge(
      String name,
      final Iterable<Tag> tags,
      final T stateObject,
      final ToDoubleFunction<T> valueFunction) {
    name = prefix + name;
    if (!registered.contains(name)) {
      registry.gauge(name, tags, stateObject, valueFunction);
      registered.add(name);
    }
  }

  public double getGauge(String key) {
    Number gauge = this.gauges.get(key);
    if (gauge == null) {
      return Double.NaN;
    }
    return gauge.doubleValue();
  }

}
