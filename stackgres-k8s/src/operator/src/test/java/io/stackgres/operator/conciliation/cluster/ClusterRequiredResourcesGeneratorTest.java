/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.prometheus.PrometheusConfig;
import io.stackgres.common.prometheus.ServiceMonitor;
import io.stackgres.common.resource.BackupConfigFinder;
import io.stackgres.common.resource.BackupFinder;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import io.stackgres.operator.resource.PrometheusScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterRequiredResourcesGeneratorTest {

  @InjectMock
  BackupConfigFinder backupConfigFinder;

  @InjectMock
  PostgresConfigFinder postgresConfigFinder;

  @InjectMock
  ProfileConfigFinder profileConfigFinder;

  @InjectMock
  PoolingConfigFinder poolingConfigFinder;

  @InjectMock
  BackupFinder backupFinder;

  @InjectMock
  PrometheusScanner prometheusScanner;

  @InjectMock
  SecretFinder secretFinder;

  @Inject
  ClusterRequiredResourcesGenerator generator;

  private StackGresCluster cluster;
  private StackGresBackupConfig backupConfig;
  private StackGresPostgresConfig postgresConfig;
  private StackGresPoolingConfig poolingConfig;
  private StackGresProfile instanceProfile;
  private StackGresBackup backup;
  private Secret minioSecret;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().getLatestVersion());
    final String namespace = cluster.getMetadata().getNamespace();
    backupConfig = Fixtures.backupConfig().loadDefault().get();
    setNamespace(backupConfig);
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    postgresConfig.getSpec()
        .setPostgresVersion(StackGresComponent.POSTGRESQL.getLatest()
            .getLatestMajorVersion());
    setNamespace(postgresConfig);
    postgresConfig.setStatus(new StackGresPostgresConfigStatus());
    final String version = postgresConfig.getSpec().getPostgresVersion();
    postgresConfig.getStatus()
        .setDefaultParameters(PostgresDefaultValues.getDefaultValues(version));
    poolingConfig = Fixtures.poolingConfig().loadDefault().get();
    setNamespace(poolingConfig);
    poolingConfig.setStatus(new StackGresPoolingConfigStatus());
    poolingConfig.getStatus().setPgBouncer(new StackGresPoolingConfigPgBouncerStatus());
    poolingConfig.getStatus().getPgBouncer()
        .setDefaultParameters(PgBouncerDefaultValues.getDefaultValues());
    instanceProfile = Fixtures.instanceProfile().loadSizeS().get();
    instanceProfile.getMetadata().setNamespace(namespace);
    setNamespace(instanceProfile);
    backup = Fixtures.backup().loadDefault().get();
    setNamespace(backup);
    minioSecret = Fixtures.secret().loadMinio().get();
  }

  private void setNamespace(HasMetadata resource) {
    resource.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
  }

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidCluster_getRequiredResourcesAllReturnedResourcesShouldHaveTheOwnerReference() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

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

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithoutBackupConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    clusterConfiguration.setBackupConfig(null);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithNoPoolingConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    clusterConfiguration.setConnectionPoolingConfig(null);

    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidClusterWithoutRestoreData_getRequiredResourcesShouldNotScanForBackups() {

    cluster.getSpec().getInitData().setRestore(null);

    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
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

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();

    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  private void mockPoolingConfig(String clusterNamespace, String connectionPoolingConfig) {
    when(poolingConfigFinder.findByNameAndNamespace(connectionPoolingConfig, clusterNamespace))
        .thenReturn(Optional.of(poolingConfig));
  }

  private void mockPgConfig(String clusterNamespace, String postgresConfigName) {
    when(postgresConfigFinder.findByNameAndNamespace(postgresConfigName, clusterNamespace))
        .thenReturn(Optional.of(this.postgresConfig));
  }

  @Test
  void givenAClusterWithInvalidPgConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    when(postgresConfigFinder.findByNameAndNamespace(postgresConfigName, clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig " + postgresConfigName);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
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

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGCluster " + clusterNamespace + "."
        + clusterName + " have a non existent SGInstanceProfile " + resourceProfile);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoBackupConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, clusterNamespace))
        .thenReturn(Optional.empty());
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoPoolingConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    when(poolingConfigFinder.findByNameAndNamespace(connectionPoolingConfig, clusterNamespace))
        .thenReturn(Optional.empty());
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenADefaultPrometheusInstallation_shouldGenerateServiceMonitors() {
    System.setProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName(), "true");
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

    when(prometheusScanner.findResources()).thenReturn(Optional.of(
        Fixtures.prometheusList().loadDefault().get().getItems()));

    List<HasMetadata> generatedResources = generator.getRequiredResources(cluster);

    var serviceMonitors = generatedResources.stream()
        .filter(r -> r.getKind().equals(ServiceMonitor.KIND))
        .count();

    assertEquals(2, serviceMonitors);
    verify(prometheusScanner).findResources();
    System.clearProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName());
  }

  @Test
  void givenAPrometheusInstallationWithNoServiceMonitorSelector_shouldGenerateServiceMonitors() {
    System.setProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName(), "true");
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();
    mockBackupConfig(clusterNamespace, backupConfigName);
    final String postgresConfigName = clusterConfiguration.getPostgresConfig();
    mockPgConfig(clusterNamespace, postgresConfigName);
    final String connectionPoolingConfig = clusterConfiguration.getConnectionPoolingConfig();
    mockPoolingConfig(clusterNamespace, connectionPoolingConfig);
    final String resourceProfile = clusterSpec.getResourceProfile();
    when(profileConfigFinder.findByNameAndNamespace(resourceProfile, clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets(clusterNamespace);

    List<PrometheusConfig> listPrometheus = Fixtures.prometheusList().loadDefault().get()
            .getItems()
            .stream()
            .peek(pc -> pc.getSpec().setServiceMonitorSelector(null))
            .toList();

    when(prometheusScanner.findResources()).thenReturn(Optional.of(listPrometheus));

    List<HasMetadata> generatedResources = generator.getRequiredResources(cluster);

    var serviceMonitors = generatedResources.stream()
        .filter(r -> r.getKind().equals(HasMetadata.getKind(ServiceMonitor.class)))
        .count();

    assertEquals(2, serviceMonitors);
    System.clearProperty(OperatorProperty.PROMETHEUS_AUTOBIND.getPropertyName());
  }

  private void mockSecrets(String clusterNamespace) {
    when(secretFinder.findByNameAndNamespace("minio", clusterNamespace))
        .thenReturn(Optional.of(minioSecret));
  }

  private void mockBackupConfig(String clusterNamespace, String backupConfigName) {
    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, clusterNamespace))
        .thenReturn(Optional.of(this.backupConfig));
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(cluster));
    assertEquals(message, ex.getMessage());
  }

}
