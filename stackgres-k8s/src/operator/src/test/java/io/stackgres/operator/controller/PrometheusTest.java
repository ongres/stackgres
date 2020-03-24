/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.customresource.prometheus.PrometheusConfig;
import io.stackgres.operator.customresource.prometheus.PrometheusConfigList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PrometheusTest {

  private StackGresCluster cluster;

  @Mock
  private KubernetesClientFactory clientFactory;

  @Mock
  private ObjectMapperProvider objectMapperProvider;

  @Mock
  private KubernetesClient client;

  @Mock
  private CustomResourceScanner<PrometheusConfig> prometheusScanner;

  @Mock
  private ConfigContext configContext;


  private PrometheusConfigList prometheusConfigList;

  private ClusterReconciliationCycle reconciliationCycle;

  @BeforeEach
  void setUp() {

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    prometheusConfigList = JsonUtil.readFromJson("prometheus/prometheus_list.json",
        PrometheusConfigList.class);

    reconciliationCycle = new ClusterReconciliationCycle(
        clientFactory, null, null, null, null, null, objectMapperProvider,
        prometheusScanner, configContext);
  }

  @Test
  void testPrometheusParsing() {

    JsonUtil.readFromJson("prometheus/prometheus_list.json", PrometheusConfigList.class);

  }

  private Prometheus invokeGetConfig() {
    Optional<Prometheus> prometheus = reconciliationCycle.getPrometheus(cluster, client);
    if(prometheus.isPresent()){
      return prometheus.get();
    } else {
      fail("should no return an empty prometheus in any case");
      return null;
    }
  }

  @Test
  void givenNoPrometheusInTheClusterAndAutobindSettled_itShouldNotFlagTheCreationOfServiceMonitor() {

    when(prometheusScanner.findResources()).thenReturn(Optional.empty());
    when(configContext.getProperty(ConfigProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    Prometheus prometheus = invokeGetConfig();

    assertFalse(prometheus.getCreateServiceMonitor());

    assertNull(prometheus.getPrometheusInstallations());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenAutobindSettledToFalse_ItShouldNotEvenLookForPrometheusInstallations() {

    when(configContext.getProperty(ConfigProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    cluster.getSpec().setPrometheusAutobind(false);

    Prometheus prometheus = invokeGetConfig();

    assertFalse(prometheus.getCreateServiceMonitor());
    verify(prometheusScanner, never()).findResources();
  }

  @Test
  void givenPrometheusInTheClusterAndAutobindSettled_itShouldReturnThePrometheusInstallations() {

    when(prometheusScanner.findResources()).thenReturn(Optional.of(prometheusConfigList.getItems()));

    when(configContext.getProperty(ConfigProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    Prometheus prometheus = invokeGetConfig();

    assertTrue(prometheus.getCreateServiceMonitor());

    assertEquals(1, prometheus.getPrometheusInstallations().size());

    PrometheusConfig promethueusConfig = prometheusConfigList.getItems().get(0);

    assertEquals(prometheus.getPrometheusInstallations().get(0).getNamespace(), promethueusConfig.getMetadata().getNamespace());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenPrometheusInTheClusterButNotMatchLabelConfiguredAndAutobindSettled__itShouldNotFlagTheCreationOfServiceMonitor() {

    when(configContext.getProperty(ConfigProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    when(prometheusScanner.findResources()).thenReturn(Optional.of(prometheusConfigList.getItems()));

    prometheusConfigList.getItems().get(0).getSpec().getServiceMonitorSelector()
        .setMatchLabels(new HashMap<>());

    Prometheus prometheus = invokeGetConfig();

    assertFalse(prometheus.getCreateServiceMonitor());

    assertNull(prometheus.getPrometheusInstallations());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenAutobindSettledButNotAllowed_ItShouldNotEvenLookForPrometheusInstallations() {

    when(configContext.getProperty(ConfigProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.FALSE.toString()));

    Prometheus prometheus = invokeGetConfig();

    assertFalse(prometheus.getCreateServiceMonitor());

    assertNull(prometheus.getPrometheusInstallations());

    verify(prometheusScanner, never()).findResources();
  }
}
