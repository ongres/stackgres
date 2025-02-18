/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.external.prometheus.Prometheus;
import io.stackgres.common.crd.external.prometheus.PrometheusBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorPrometheusOperatorBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.PrometheusContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigPrometheusContextAppenderTest {

  private ConfigPrometheusContextAppender contextAppender;

  private StackGresConfig config;

  @Spy
  private StackGresConfigContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<Prometheus> prometheusScanner;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    config.getSpec().setCollector(new StackGresConfigCollector());
    config.getSpec().getCollector().setPrometheusOperator(
        new StackGresConfigCollectorPrometheusOperatorBuilder()
        .withAllowDiscovery(true)
        .build());
    contextAppender = new ConfigPrometheusContextAppender(prometheusScanner);
  }

  @Test
  void givenConfigWithoutAllowDiscovery_shouldPass() {
    config.getSpec().getCollector().getPrometheusOperator().setAllowDiscovery(false);

    contextAppender.appendContext(config, contextBuilder);

    verify(prometheusScanner, Mockito.never()).findResources();
    verify(contextBuilder).prometheus(List.of());
  }

  @Test
  void givenConfigWithAllowDiscovery_shouldPass() {
    contextAppender.appendContext(config, contextBuilder);

    verify(prometheusScanner).findResources();
    verify(contextBuilder).prometheus(List.of());
  }

  @Test
  void givenConfigWithAllowDiscoveryAndPrometheusFound_shouldPass() {
    Prometheus prometheus =
        new PrometheusBuilder()
        .build();
    when(prometheusScanner.findResources())
        .thenReturn(Optional.of(List.of(prometheus)));

    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).prometheus(
        List.of(PrometheusContext.toPrometheusContext(prometheus)));
  }

  @Test
  void givenConfigTargetingPrometheusAndMonitors_shouldPass() {
    config.getSpec().getCollector().setPrometheusOperator(
        new StackGresConfigCollectorPrometheusOperatorBuilder()
        .withAllowDiscovery(true)
        .addNewMonitor()
        .withName("target-prometheus")
        .withNamespace("target-namespace")
        .endMonitor()
        .build());
    Prometheus targetPrometheus =
        new PrometheusBuilder()
        .withNewMetadata()
        .withName("target-prometheus")
        .withNamespace("target-namespace")
        .endMetadata()
        .build();
    Prometheus otherPrometheus =
        new PrometheusBuilder()
        .withNewMetadata()
        .withName("other-prometheus")
        .withNamespace("other-namespace")
        .endMetadata()
        .build();
    when(prometheusScanner.findResources())
        .thenReturn(Optional.of(List.of(targetPrometheus, otherPrometheus)));

    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).prometheus(
        List.of(PrometheusContext.toPrometheusContext(
            targetPrometheus,
            config.getSpec().getCollector().getPrometheusOperator().getMonitors().get(0))));
  }

}
