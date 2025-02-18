/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.stackgres.common.crd.external.prometheus.Prometheus;
import io.stackgres.common.crd.external.prometheus.PrometheusSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorPrometheusOperatorMonitor;

public class PrometheusContext {

  private final String namespace;

  private final String name;

  private final Map<String, String> matchLabels;

  private final StackGresConfigCollectorPrometheusOperatorMonitor monitor;

  public PrometheusContext(
      String namespace,
      String name,
      Map<String, String> matchLabels,
      StackGresConfigCollectorPrometheusOperatorMonitor monitor) {
    this.namespace = namespace;
    this.name = name;
    this.matchLabels = matchLabels;
    this.monitor = monitor;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getMatchLabels() {
    return matchLabels;
  }

  public Optional<StackGresConfigCollectorPrometheusOperatorMonitor> getMonitor() {
    return Optional.ofNullable(monitor);
  }

  public static PrometheusContext toPrometheusContext(Prometheus prometheus) {
    Map<String, String> matchLabels = Optional.ofNullable(prometheus.getSpec())
        .map(PrometheusSpec::getPodMonitorSelector)
        .map(LabelSelector::getMatchLabels)
        .map(Map::copyOf)
        .orElse(Map.of());
    return new PrometheusContext(
        prometheus.getMetadata().getNamespace(),
        prometheus.getMetadata().getName(),
        matchLabels,
        null);
  }

  public static PrometheusContext toPrometheusContext(
      Prometheus prometheus,
      StackGresConfigCollectorPrometheusOperatorMonitor monitor) {
    Map<String, String> matchLabels = Optional.ofNullable(prometheus.getSpec())
        .map(PrometheusSpec::getPodMonitorSelector)
        .map(LabelSelector::getMatchLabels)
        .map(Map::copyOf)
        .orElse(Map.of());
    return new PrometheusContext(
        prometheus.getMetadata().getNamespace(),
        prometheus.getMetadata().getName(),
        matchLabels,
        monitor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(matchLabels, monitor, name, namespace);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PrometheusContext)) {
      return false;
    }
    PrometheusContext other = (PrometheusContext) obj;
    return Objects.equals(matchLabels, other.matchLabels) && Objects.equals(monitor, other.monitor)
        && Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace);
  }

}
