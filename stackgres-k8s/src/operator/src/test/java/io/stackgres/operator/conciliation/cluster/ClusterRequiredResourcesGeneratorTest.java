/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
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
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidCluster_getRequiredResourcesAllReturnedResourcesShouldHaveTheOwnerReference() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    List<HasMetadata> resources = generator.getRequiredResources(cluster);

    resources.forEach(resource -> {
      assertNotNull(resource.getMetadata().getOwnerReferences(),
          "Resource " + resource.getMetadata().getName() + " doesn't owner references");
      if (resource.getMetadata().getOwnerReferences().size() == 0) {
        fail("Resource " + resource.getMetadata().getName() + " doesn't have any owner");
      }
      assertTrue(resource.getMetadata().getOwnerReferences().stream()
          .anyMatch(ownerReference -> ownerReference.getApiVersion()
              .equals(HasMetadata.getApiVersion(StackGresCluster.class))
              && ownerReference.getKind().equals(HasMetadata.getKind(StackGresCluster.class))
              && ownerReference.getName().equals(cluster.getMetadata().getName())
              && ownerReference.getUid().equals(cluster.getMetadata().getUid())
              && Optional.ofNullable(ownerReference.getBlockOwnerDeletion()).orElse(Boolean.FALSE)
                  .equals(Boolean.FALSE)));
    });

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithoutBackupConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    cluster.getSpec().getConfiguration().setBackupConfig(null);
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithNoPoolingConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    cluster.getSpec().getConfiguration().setConnectionPoolingConfig(null);

    mockBackupConfig();
    mockPgConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithoutRestoreData_getRequiredResourcesShouldNotScanForBackups() {

    cluster.getSpec().getInitData().setRestore(null);

    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithMissingRestoreBackup_getRequiredResourcesShouldNotFail() {

    cluster.getSpec().getInitData().getRestore()
        .setFromBackup(new StackGresClusterRestoreFromBackup());
    cluster.getSpec().getInitData().getRestore().getFromBackup()
        .setName(backup.getMetadata().getName());

    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithInvalidPgConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig "
        + cluster.getSpec().getConfiguration().getPostgresConfig());

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(profileConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithoutInstanceProfile_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    mockPgConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGCluster " + clusterNamespace + "."
        + clusterName + " have a non existent SGInstanceProfile " + resourceProfile);

    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoBackupConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    when(backupConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoPoolingConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenADefaultPrometheusInstallation_shouldGeneratePodMonitors() {
    System.setProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName(), "true");
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
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
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    final String resourceProfile = cluster.getSpec().getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
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
