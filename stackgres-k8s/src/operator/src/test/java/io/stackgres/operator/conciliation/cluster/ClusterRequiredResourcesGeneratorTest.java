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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
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
import io.stackgres.common.prometheus.PrometheusConfigList;
import io.stackgres.common.prometheus.ServiceMonitor;
import io.stackgres.common.resource.BackupConfigFinder;
import io.stackgres.common.resource.BackupScanner;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import io.stackgres.operator.resource.PrometheusScanner;
import io.stackgres.testutil.JsonUtil;
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
  BackupScanner backupScanner;

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
  private List<StackGresBackup> backups;
  private Secret minioSecret;

  @BeforeEach
  void setUp() {
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL.findLatestVersion());
    final String namespace = cluster.getMetadata().getNamespace();
    backupConfig = JsonUtil.readFromJson("backup_config/default.json", StackGresBackupConfig.class);
    setNamespace(backupConfig);
    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
    postgresConfig.getSpec()
        .setPostgresVersion(StackGresComponent.POSTGRESQL.findLatestMajorVersion());
    setNamespace(postgresConfig);
    postgresConfig.setStatus(new StackGresPostgresConfigStatus());
    final String version = postgresConfig.getSpec().getPostgresVersion();
    postgresConfig.getStatus()
        .setDefaultParameters(PostgresDefaultValues.getDefaultValues(version));
    poolingConfig =
        JsonUtil.readFromJson("pooling_config/default.json", StackGresPoolingConfig.class);
    setNamespace(poolingConfig);
    poolingConfig.setStatus(new StackGresPoolingConfigStatus());
    poolingConfig.getStatus().setPgBouncer(new StackGresPoolingConfigPgBouncerStatus());
    poolingConfig.getStatus().getPgBouncer()
        .setDefaultParameters(PgBouncerDefaultValues.getDefaultValues());
    instanceProfile =
        JsonUtil.readFromJson("stackgres_profiles/size-s.json", StackGresProfile.class);
    instanceProfile.getMetadata().setNamespace(namespace);
    setNamespace(instanceProfile);
    backups = JsonUtil.readFromJson("backup/list.json", StackGresBackupList.class).getItems();
    backups.forEach(this::setNamespace);
    minioSecret = JsonUtil.readFromJson("secret/minio.json", Secret.class);
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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner, atLeastOnce()).getResources();
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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    List<HasMetadata> resources = generator.getRequiredResources(cluster);

    resources.forEach(resource -> {
      assertNotNull(resource.getMetadata().getOwnerReferences(),
          "Resource " + resource.getMetadata().getName() + " doesn't owner references");
      if (resource.getMetadata().getOwnerReferences().size() == 0) {
        fail("Resource " + resource.getMetadata().getName() + " doesn't have any owner");
      }
      assertTrue(resource.getMetadata().getOwnerReferences().stream().anyMatch(ownerReference
          -> ownerReference.getApiVersion().equals(HasMetadata.getApiVersion(
              StackGresCluster.class))
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
    verify(backupScanner, atLeastOnce()).getResources();
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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner, atLeastOnce()).getResources();
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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner, atLeastOnce()).getResources();
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
    verify(backupScanner, never()).getResources();
  }

  @Test
  void givenAClusterUnknownRestoreData_getRequiredResourcesShouldNotFail() {

    cluster.getSpec().getInitData().getRestore()
        .setFromBackup(new StackGresClusterRestoreFromBackup());
    cluster.getSpec().getInitData().getRestore().getFromBackup()
        .setUid(UUID.randomUUID().toString());

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
    when(backupScanner.getResources()).thenReturn(backups);
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner, atLeastOnce()).getResources();
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

    assertException("SGCluster " + clusterNamespace + "/" + clusterName
        + " have a non existent SGPostgresConfig " + postgresConfigName);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(profileConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupScanner, never()).getResources();
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

    assertException("SGCluster " + clusterNamespace + "/"
        + clusterName + " have a non existent SGInstanceProfile " + resourceProfile);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupScanner, never()).getResources();
  }

  @Test
  void givenClusterWithNoBackupConfig_NotFail() {
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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner, atLeastOnce()).getResources();
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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner, atLeastOnce()).getResources();
  }

  @Test
  void givenADefaultPrometheusInstallation_shouldGenerateServiceMonitors() {

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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    when(prometheusScanner.findResources()).thenReturn(Optional.of(
        JsonUtil.readFromJson("prometheus/prometheus_list.json", PrometheusConfigList.class)
            .getItems()));

    List<HasMetadata> generatedResources = generator.getRequiredResources(cluster);

    var serviceMonitors = generatedResources.stream()
        .filter(r -> r.getKind().equals(ServiceMonitor.KIND))
        .collect(Collectors.toUnmodifiableList());

    assertEquals(2, serviceMonitors.size());
    verify(prometheusScanner).findResources();
  }

  @Test
  void givenAPrometheusInstallationWithNoServiceMonitorSelector_shouldGenerateServiceMonitors() {

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
    when(backupScanner.getResources())
        .thenReturn(backups);
    mockSecrets(clusterNamespace);

    when(prometheusScanner.findResources()).thenReturn(Optional.of(
        JsonUtil.readFromJson("prometheus/prometheus_list.json", PrometheusConfigList.class)
            .getItems()
            .stream().peek(pc -> pc.getSpec().setServiceMonitorSelector(null))
            .collect(Collectors.toUnmodifiableList())));

    List<HasMetadata> generatedResources = generator.getRequiredResources(cluster);

    var serviceMonitors = generatedResources.stream()
        .filter(r -> r.getKind().equals(ServiceMonitor.KIND))
        .collect(Collectors.toUnmodifiableList());

    assertEquals(2, serviceMonitors.size());
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
