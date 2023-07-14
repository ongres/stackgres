/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.BackupConfigFinder;
import io.stackgres.common.resource.BackupFinder;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import io.stackgres.operator.resource.PrometheusScanner;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

abstract class AbstractClusterRequiredResourcesGeneratorTest {

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ObjectStorageFinder objectStorageFinder;

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

  StackGresCluster cluster;
  StackGresBackupConfig backupConfig;
  StackGresPostgresConfig postgresConfig;
  StackGresPoolingConfig poolingConfig;
  StackGresProfile instanceProfile;
  StackGresBackup backup;
  Secret minioSecret;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().streamOrderedVersions()
        .skipWhile(version -> version.startsWith("15")).findFirst().orElseThrow());
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    final String namespace = cluster.getMetadata().getNamespace();
    backupConfig = Fixtures.backupConfig().loadDefault().get();
    setNamespace(backupConfig);
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    postgresConfig.getSpec()
        .setPostgresVersion(StackGresComponent.POSTGRESQL
            .getLatest().streamOrderedMajorVersions()
            .skipWhile(version -> version.startsWith("15")).findFirst().orElseThrow());
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

  void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(cluster));
    assertEquals(message, ex.getMessage());
  }

  protected void mockProfile() {
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getResourceProfile(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(instanceProfile));
  }

  void mockPoolingConfig() {
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getConnectionPoolingConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(poolingConfig));
  }

  void mockPgConfig() {
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getPostgresConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.postgresConfig));
  }

  void unmockBackupConfig() {
    cluster.getSpec().getConfiguration().setBackups(null);
    cluster.getSpec().getConfiguration().setBackupConfig(null);
  }

  void mockBackupConfig() {
    when(backupConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfiguration().getBackupConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.backupConfig));
  }

  void mockSecrets() {
    when(secretFinder.findByNameAndNamespace(
        "minio",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(minioSecret));
  }

}
