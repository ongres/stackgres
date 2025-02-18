/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.external.prometheus.Prometheus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorPrometheusOperator;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.PrometheusContext;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigPrometheusContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(ConfigPrometheusContextAppender.class);

  private final CustomResourceScanner<Prometheus> prometheusScanner;

  public ConfigPrometheusContextAppender(CustomResourceScanner<Prometheus> prometheusScanner) {
    this.prometheusScanner = prometheusScanner;
  }

  public void appendContext(StackGresConfig config, Builder contextBuilder) {
    final List<PrometheusContext> prometheus = getPrometheus(config);
    contextBuilder.prometheus(prometheus);
  }

  public List<PrometheusContext> getPrometheus(StackGresConfig config) {
    boolean isAutobindAllowed = Optional.of(config)
        .map(StackGresConfig::getSpec)
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getPrometheusOperator)
        .map(StackGresConfigCollectorPrometheusOperator::getAllowDiscovery)
        .orElse(false);
    var monitors = Optional.of(config)
        .map(StackGresConfig::getSpec)
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getPrometheusOperator)
        .map(StackGresConfigCollectorPrometheusOperator::getMonitors)
        .orElse(List.of());
        
    if (monitors.size() > 0) {
      LOGGER.trace("Prometheus monitors detected, looking for Prometheus resources");
      return prometheusScanner.findResources()
          .stream()
          .flatMap(List::stream)
          .filter(prometheus -> monitors.stream()
              .anyMatch(monitor -> monitor.getNamespace().equals(prometheus.getMetadata().getNamespace())
                  && monitor.getName().equals(prometheus.getMetadata().getName())))
          .map(prometheus -> PrometheusContext.toPrometheusContext(
              prometheus,
              monitors.stream()
              .filter(monitor -> monitor.getNamespace().equals(prometheus.getMetadata().getNamespace())
                  && monitor.getName().equals(prometheus.getMetadata().getName()))
              .findFirst()
              .orElseThrow()))
          .toList();
    } else if (isAutobindAllowed) {
      LOGGER.trace("Prometheus auto bind enabled, looking for Prometheus resources");

      return prometheusScanner.findResources()
          .stream()
          .flatMap(List::stream)
          .map(PrometheusContext::toPrometheusContext)
          .toList();
    } else {
      return List.of();
    }
  }

}
