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

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.customresource.prometheus.PrometheusConfig;
import io.stackgres.operator.customresource.prometheus.PrometheusConfigList;
import io.stackgres.testutil.JsonUtil;
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
  private CustomResourceScanner<StackGresCluster> clusterScanner;

  @Mock
  private CustomResourceFinder<StackGresProfile> profileFinder;

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @Mock
  private CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  @Mock
  private CustomResourceScanner<StackGresBackup> backupScanner;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private CustomResourceScanner<PrometheusConfig> prometheusScanner;

  @Mock
  private OperatorPropertyContext operatorContext;

  @Mock
  private LabelFactory<StackGresCluster> labelFactory;


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
        operatorContext, labelFactory, clusterScanner, profileFinder, postgresConfigFinder,
        backupConfigFinder, backupScanner, secretFinder, prometheusScanner);
  }

  @Test
  void testPrometheusParsing() {

    JsonUtil.readFromJson("prometheus/prometheus_list.json", PrometheusConfigList.class);

  }

  private Prometheus invokeGetConfig() {
    Optional<Prometheus> prometheus = reconciliationCycle.getPrometheus(cluster);
    if (prometheus.isPresent()) {
      return prometheus.get();
    } else {
      fail("should no return an empty prometheus in any case");
      return null;
    }
  }

  @Test
  void givenNoPrometheusInTheClusterAndAutobindSettled_itShouldNotFlagTheCreationOfServiceMonitor() {

    when(prometheusScanner.findResources()).thenReturn(Optional.empty());
    when(operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(true);

    Prometheus prometheus = invokeGetConfig();

    assertFalse(prometheus.getCreateServiceMonitor());

    assertNull(prometheus.getPrometheusInstallations());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenAutobindSettledToFalse_ItShouldNotEvenLookForPrometheusInstallations() {

    when(operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(true);

    cluster.getSpec().setPrometheusAutobind(false);

    Prometheus prometheus = invokeGetConfig();

    assertFalse(prometheus.getCreateServiceMonitor());
    verify(prometheusScanner, never()).findResources();
  }

  @Test
  void givenPrometheusInTheClusterAndAutobindSettled_itShouldReturnThePrometheusInstallations() {

    when(prometheusScanner.findResources()).thenReturn(Optional.of(prometheusConfigList.getItems()));

    when(operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(true);

    Prometheus prometheus = invokeGetConfig();

    assertTrue(prometheus.getCreateServiceMonitor());

    assertEquals(1, prometheus.getPrometheusInstallations().size());

    PrometheusConfig promethueusConfig = prometheusConfigList.getItems().get(0);

    assertEquals(prometheus.getPrometheusInstallations().get(0).getNamespace(), promethueusConfig.getMetadata().getNamespace());

    verify(prometheusScanner).findResources();

  }

  @Test
  void givenPrometheusInTheClusterButNotMatchLabelConfiguredAndAutobindSettled__itShouldNotFlagTheCreationOfServiceMonitor() {

    when(operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(true);

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

    when(operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND))
        .thenReturn(false);

    Prometheus prometheus = invokeGetConfig();

    assertFalse(prometheus.getCreateServiceMonitor());

    assertNull(prometheus.getPrometheusInstallations());

    verify(prometheusScanner, never()).findResources();
  }
}