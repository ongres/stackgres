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
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternal;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.prometheus.PodMonitor;
import io.stackgres.common.prometheus.PrometheusConfig;
import io.stackgres.common.resource.BackupConfigFinder;
import io.stackgres.common.resource.BackupFinder;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import io.stackgres.operator.resource.PrometheusScanner;
import io.stackgres.operatorframework.resource.ResourceUtil;
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
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
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

  @Test
  void givenClusterWithReplicateFromExternal_shouldReadUsersSecrets() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternal();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
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
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutSecretForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutSecret(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutSecretForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutSecret(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutSecretForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutSecret(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutSecretForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutSecret(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutSecretForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutSecretForAuthenticatorpassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutSecret(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password secret missing-test-secret was not found");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutKeyForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutKeyForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutKeyForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutKeyForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutKeyForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromExternalWithoutKeyForAuthenticatorpassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromExternalWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test-secret");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromCluster_shouldReadUsersSecrets() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromCluster();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
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
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecret_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutSecret();
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Can not find secret test"
        + " for SGCluster test"
        + " to replicate from");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForSuperuserUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser username key PATRONI_SUPERUSER_USERNAME"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForSuperuserPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Superuser password key PATRONI_SUPERUSER_PASSWORD"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForReplicationUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.REPLICATION_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication username key PATRONI_REPLICATION_USERNAME"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForReplicationPassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Replication password key PATRONI_REPLICATION_PASSWORD"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForAuthenticatorUsername_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator username key PATRONI_authenticator_USERNAME"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithReplicateFromClusterWithoutSecretForAuthenticatorpassword_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockReplicateFromClusterWithoutKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV);
    mockBackupConfig();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(cluster.getSpec()
        .getConfiguration().getConnectionPoolingConfig(), clusterNamespace))
        .thenReturn(Optional.empty());
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace))
        .thenReturn(Optional.of(instanceProfile));
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    assertException("Authenticator password key PATRONI_authenticator_PASSWORD"
        + " was not found in secret test");

    verify(backupConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(), clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(), clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(), clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(), clusterNamespace);
    verify(backupFinder).findByNameAndNamespace(any(), any());
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(cluster));
    assertEquals(message, ex.getMessage());
  }

  private void mockPoolingConfig() {
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(poolingConfig));
  }

  private void mockPgConfig() {
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.postgresConfig));
  }

  private void mockBackupConfig() {
    when(backupConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.backupConfig));
  }

  private void mockSecrets() {
    when(secretFinder.findByNameAndNamespace(
        "minio",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(minioSecret));
  }

  private void mockReplicateFromExternal() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal().setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal().setPort(5433);
    cluster.getSpec().getReplicateFrom().setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromExternalWithoutSecret(String key) {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal().setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal().setPort(5433);
    cluster.getSpec().getReplicateFrom().setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
            key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
            ? "missing-test-secret" : "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
            key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
            ? "missing-test-secret" : "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromExternalWithoutKey(String key) {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    cluster.getSpec().getReplicateFrom().getInstance().getExternal().setHost("test");
    cluster.getSpec().getReplicateFrom().getInstance().getExternal().setPort(5433);
    cluster.getSpec().getReplicateFrom().setUsers(new StackGresClusterReplicateFromUsers());
    cluster.getSpec().getReplicateFrom().getUsers()
        .setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getSuperuser().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getReplication().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .setAuthenticator(new StackGresClusterReplicateFromUserSecretKeyRef());
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV, "test-secret"));
    cluster.getSpec().getReplicateFrom().getUsers()
        .getAuthenticator().setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV, "test-secret"));
    when(secretFinder.findByNameAndNamespace(
        "test-secret",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromCluster() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.readWriteName("test"),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

  private void mockReplicateFromClusterWithoutSecret() {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.readWriteName("test"),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
  }

  private void mockReplicateFromClusterWithoutKey(String key) {
    cluster.getSpec().setReplicateFrom(new StackGresClusterReplicateFrom());
    cluster.getSpec().getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
    cluster.getSpec().getReplicateFrom().getInstance()
        .setSgCluster("test");
    when(secretFinder.findByNameAndNamespace(
        PatroniUtil.readWriteName("test"),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(new SecretBuilder()
            .withData(Map.ofEntries(
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("postgres")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("replicator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
                    ResourceUtil.encodeSecret("authenticator")),
                Map.entry(
                    key.equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
                    ? "missing-key" : StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
                    ResourceUtil.encodeSecret("authenticator"))))
            .build()));
  }

}
