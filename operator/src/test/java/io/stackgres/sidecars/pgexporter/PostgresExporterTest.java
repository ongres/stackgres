/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter;

import java.util.HashMap;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.config.ConfigContext;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.sidecars.pgexporter.customresources.StackGresPostgresExporterConfig;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfig;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PostgresExporterTest {

  private StackGresCluster cluster;

  @Mock
  private KubernetesClient client;

  @Mock
  private KubernetesScanner<PrometheusConfigList> prometheusScanner;

  @Mock
  private ConfigContext configContext;


  private PrometheusConfigList prometheusConfigList;

  private PostgresExporter exporter;

  @BeforeEach
  void setUp() {

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    prometheusConfigList = JsonUtil.readFromJson("prometheus/prometheus_list.json",
        PrometheusConfigList.class);


    exporter = new PostgresExporter(prometheusScanner, configContext);

  }

  @Test
  void testPrometheusParsing() {

    JsonUtil.readFromJson("prometheus/prometheus_list.json", PrometheusConfigList.class);

  }


  private StackGresPostgresExporterConfig invokeGetConfig() {


    Optional<StackGresPostgresExporterConfig> config = exporter.getConfig(cluster, client);

    if(config.isPresent()){
      return config.get();
    } else {
      fail("the exporter should no " +
          "return an empty config in any case");
      return null;
    }
  }


  @Test
  void givenNoPrometheusInTheClusterAndAutobindSettled_itShouldNotFlagTheCreationOfServiceMonitor() {

    when(prometheusScanner.findResources()).thenReturn(Optional.empty());
    when(configContext.getProp(ConfigContext.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    StackGresPostgresExporterConfig exporterConfig = invokeGetConfig();

    assertFalse(exporterConfig.getSpec().getCreateServiceMonitor());

    assertNull(exporterConfig.getSpec().getPrometheusInstallations());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenAutobindSettledToFalse_ItShouldNotEvenLookForPrometheusInstallations() {

    when(configContext.getProp(ConfigContext.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    cluster.getSpec().setPrometheusAutobind(false);

    StackGresPostgresExporterConfig exporterConfig = invokeGetConfig();

    assertFalse(exporterConfig.getSpec().getCreateServiceMonitor());
    verify(prometheusScanner, never()).findResources();
  }

  @Test
  void givenPrometheusInTheClusterAndAutobindSettled_itShouldReturnThePrometheusInstallations() {

    when(prometheusScanner.findResources()).thenReturn(Optional.of(prometheusConfigList));

    when(configContext.getProp(ConfigContext.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    StackGresPostgresExporterConfig exporterConfig = invokeGetConfig();

    assertTrue(exporterConfig.getSpec().getCreateServiceMonitor());

    assertEquals(1, exporterConfig.getSpec().getPrometheusInstallations().size());

    PrometheusConfig promethueusConfig = prometheusConfigList.getItems().get(0);

    assertEquals(exporterConfig.getSpec().getPrometheusInstallations().get(0).getNamespace(), promethueusConfig.getMetadata().getNamespace());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenPrometheusInTheClusterButNotMatchLabelConfiguredAndAutobindSettled__itShouldNotFlagTheCreationOfServiceMonitor() {

    when(configContext.getProp(ConfigContext.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.TRUE.toString()));

    when(prometheusScanner.findResources()).thenReturn(Optional.of(prometheusConfigList));

    prometheusConfigList.getItems().get(0).getSpec().getServiceMonitorSelector()
        .setMatchLabels(new HashMap<>());

    StackGresPostgresExporterConfig exporterConfig = invokeGetConfig();

    assertFalse(exporterConfig.getSpec().getCreateServiceMonitor());

    assertNull(exporterConfig.getSpec().getPrometheusInstallations());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenAutobindSettledButNotAllowed_ItShouldNotEvenLookForPrometheusInstallations() {

    when(configContext.getProp(ConfigContext.PROMETHEUS_AUTOBIND))
        .thenReturn(Optional.of(Boolean.FALSE.toString()));

    StackGresPostgresExporterConfig exporterConfig = invokeGetConfig();

    assertFalse(exporterConfig.getSpec().getCreateServiceMonitor());

    assertNull(exporterConfig.getSpec().getPrometheusInstallations());

    verify(prometheusScanner, never()).findResources();
  }
}
