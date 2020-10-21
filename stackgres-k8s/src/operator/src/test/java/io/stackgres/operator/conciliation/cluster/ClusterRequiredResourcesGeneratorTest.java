/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.BackupConfigFinder;
import io.stackgres.common.resource.BackupScanner;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.resource.ServiceAccountFinder;
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
  ServiceAccountFinder serviceAccountFinder;

  @InjectMock
  BackupScanner backupScanner;

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
    final String namespace = cluster.getMetadata().getNamespace();
    backupConfig = JsonUtil.readFromJson("backup_config/default.json", StackGresBackupConfig.class);
    setNamespace(backupConfig);
    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json", StackGresPostgresConfig.class);
    setNamespace(postgresConfig);
    poolingConfig = JsonUtil.readFromJson("pooling_config/default.json", StackGresPoolingConfig.class);
    setNamespace(poolingConfig);
    instanceProfile = JsonUtil.readFromJson("stackgres_profiles/size-s.json", StackGresProfile.class);
    instanceProfile.getMetadata().setNamespace(namespace);
    setNamespace(instanceProfile);
    backups = JsonUtil.readFromJson("backup/list.json", StackGresBackupList.class).getItems();
    backups.forEach(this::setNamespace);
    minioSecret = JsonUtil.readFromJson("secret/minio.json", Secret.class);
  }

  private void setNamespace(HasMetadata resource){
    resource.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
  }

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    mockSecrets(clusterName, clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner).getResources();
    verify(serviceAccountFinder, times(2)).findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  @Test
  void givenValidCluster_getRequiredResourcesAllReturnedResourcesShouldHaveTheOwnerReference() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    mockSecrets(clusterName, clusterNamespace);

    List<HasMetadata> resources = generator.getRequiredResources(cluster);

    resources.forEach(resource -> {
      assertNotNull(resource.getMetadata().getOwnerReferences(),
          "Resource " + resource.getMetadata().getName() + " doesn't owner references");
      if (resource.getMetadata().getOwnerReferences().size() == 0){
        fail("Resource " + resource.getMetadata().getName() + " doesn't have any owner");
      }
    });

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner).getResources();
    verify(serviceAccountFinder, times(2))
        .findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  @Test
  void givenValidClusterWithoutBackupConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    mockSecrets(clusterName, clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner).getResources();
    verify(serviceAccountFinder, times(2))
        .findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  @Test
  void givenValidClusterWithNoPoolingConfig_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    mockSecrets(clusterName, clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner).getResources();
    verify(serviceAccountFinder, times(2))
        .findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  @Test
  void givenValidClusterWithoutRestoreData_getRequiredResourcesShouldNotScanForBackups() {

    cluster.getSpec().getInitData().setRestore(null);

    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    mockSecrets(clusterName, clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner, never()).getResources();
    verify(serviceAccountFinder, times(2))
        .findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  @Test
  void givenAClusterInvalidRestoreData_getRequiredResourcesShouldNotScanForBackups() {

    cluster.getSpec().getInitData().getRestore().getFromBackup().setUid(UUID.randomUUID().toString());

    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    when(secretFinder.findByNameAndNamespace(clusterName, clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGCluster " + clusterNamespace + "/" + clusterName + " have an invalid restore backup Uid");

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner).getResources();
    verify(serviceAccountFinder, never()).findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder, never()).findByNameAndNamespace(clusterName, clusterNamespace);

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

    assertException("SGCluster " + clusterNamespace + "/" + clusterName + " have a non existent SGPostgresConfig " + postgresConfigName);

    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(profileConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder,never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(backupScanner, never()).getResources();
    verify(serviceAccountFinder, never()).findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder, never()).findByNameAndNamespace(clusterName, clusterNamespace);

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
    verify(serviceAccountFinder, never()).findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder, never()).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  @Test
  void givenClusterWithNoBackupConfig_NotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    mockSecrets(clusterName, clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner).getResources();
    verify(serviceAccountFinder, times(2))
        .findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  @Test
  void givenClusterWithNoPoolingConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
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
    when(serviceAccountFinder.findByNameAndNamespace(any(), eq(clusterNamespace)))
        .thenReturn(Optional.empty());
    mockSecrets(clusterName, clusterNamespace);

    generator.getRequiredResources(cluster);

    verify(backupConfigFinder).findByNameAndNamespace(backupConfigName, clusterNamespace);
    verify(postgresConfigFinder).findByNameAndNamespace(postgresConfigName, clusterNamespace);
    verify(poolingConfigFinder).findByNameAndNamespace(connectionPoolingConfig, clusterNamespace);
    verify(profileConfigFinder).findByNameAndNamespace(resourceProfile, clusterNamespace);
    verify(backupScanner).getResources();
    verify(serviceAccountFinder, times(2))
        .findByNameAndNamespace(any(), eq(clusterNamespace));
    verify(secretFinder).findByNameAndNamespace(clusterName, clusterNamespace);

  }

  private void mockSecrets(String clusterName, String clusterNamespace) {
    when(secretFinder.findByNameAndNamespace("minio", clusterNamespace))
        .thenReturn(Optional.of(minioSecret));
    when(secretFinder.findByNameAndNamespace(clusterName, clusterNamespace))
        .thenReturn(Optional.empty());
  }

  private void mockBackupConfig(String clusterNamespace, String backupConfigName) {
    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, clusterNamespace))
        .thenReturn(Optional.of(this.backupConfig));
  }

  private void assertException(String message){
    var ex = assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(cluster));
    assertEquals(message, ex.getMessage());
  }

}
