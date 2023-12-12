/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.prometheus.PodMonitor;
import io.stackgres.common.prometheus.PrometheusConfig;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterRequiredResourcesGeneratorTest extends AbstractClusterRequiredResourcesGeneratorTest {

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(objectStorageFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithoutBackupConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPgConfig();
    mockPoolingConfig();
    mockProfile();
    unmockBackupConfig();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(objectStorageFinder, never()).findByNameAndNamespace(any(), any());
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithNoPoolingConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    cluster.getSpec().getConfigurations().setSgPoolingConfig(null);

    mockBackupConfig();
    mockPgConfig();
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(objectStorageFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithoutRestoreData_getRequiredResourcesShouldNotScanForBackups() {

    cluster.getSpec().getInitialData().setRestore(null);

    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getSgInstanceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(objectStorageFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithMissingRestoreBackup_getRequiredResourcesShouldNotFail() {

    cluster.getSpec().getInitialData().getRestore()
        .setFromBackup(new StackGresClusterRestoreFromBackup());
    cluster.getSpec().getInitialData().getRestore().getFromBackup()
        .setName(backup.getMetadata().getName());

    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getSgInstanceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(objectStorageFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithInvalidPgConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig "
        + cluster.getSpec().getConfigurations().getSgPostgresConfig());

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(profileConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(objectStorageFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithoutInstanceProfile_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    mockPgConfig();
    final String resourceProfile = cluster.getSpec().getSgInstanceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGCluster " + clusterNamespace + "."
        + clusterName + " have a non existent SGInstanceProfile " + resourceProfile);

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(objectStorageFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoBackupConfig_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    when(objectStorageFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        clusterNamespace))
        .thenReturn(Optional.empty());
    mockPgConfig();
    mockPoolingConfig();
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("SGObjectStorage "
        + cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage()
        + " not found");

    verify(objectStorageFinder).findByNameAndNamespace(any(), any());
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoPoolingConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(objectStorageFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenADefaultPrometheusInstallation_shouldGeneratePodMonitors() {
    System.setProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName(), "true");

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    when(prometheusScanner.findResources()).thenReturn(Optional.of(
        Fixtures.prometheusList().loadDefault().get().getItems()));

    List<HasMetadata> generatedResources = generator.getRequiredResources(cluster);

    var podMonitors = generatedResources.stream()
        .filter(r -> r.getKind().equals(PodMonitor.KIND))
        .count();

    assertEquals(2, podMonitors);
    verify(prometheusScanner).findResources();
    System.clearProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName());
  }

  @Test
  void givenAPrometheusInstallationWithNoPodMonitorSelector_shouldGeneratePodMonitors() {
    System.setProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName(), "true");

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    List<PrometheusConfig> listPrometheus = Fixtures.prometheusList().loadDefault().get()
            .getItems()
            .stream()
            .peek(pc -> pc.getSpec().setPodMonitorSelector(null))
            .toList();

    when(prometheusScanner.findResources()).thenReturn(Optional.of(listPrometheus));

    List<HasMetadata> generatedResources = generator.getRequiredResources(cluster);

    var podMonitors = generatedResources.stream()
        .filter(r -> r.getKind().equals(HasMetadata.getKind(PodMonitor.class)))
        .count();

    assertEquals(2, podMonitors);
    System.clearProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName());
  }

}
