/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static io.stackgres.operator.utils.ConciliationUtils.toNumericPostgresVersion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupInformation;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.BackupFinder;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import io.stackgres.operator.resource.PrometheusScanner;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ClusterRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ObjectStorageFinder objectStorageFinder;

  @InjectMock
  PostgresConfigFinder postgresConfigFinder;

  @InjectMock
  ProfileFinder profileFinder;

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

  StackGresConfig config;
  StackGresCluster cluster;
  StackGresObjectStorage objectStorage;
  StackGresPostgresConfig postgresConfig;
  StackGresPoolingConfig poolingConfig;
  StackGresProfile instanceProfile;
  StackGresBackup backup;
  Secret minioSecret;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().streamOrderedVersions()
        .skipWhile(version -> version.startsWith("15")).findFirst().orElseThrow());
    cluster.getStatus().setPostgresVersion(null);
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    final String namespace = cluster.getMetadata().getNamespace();
    objectStorage = Fixtures.objectStorage().loadDefault().get();
    setNamespace(objectStorage);
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
    instanceProfile = Fixtures.instanceProfile().loadSizeM().get();
    instanceProfile.getMetadata().setNamespace(namespace);
    setNamespace(instanceProfile);
    backup = Fixtures.backup().loadDefault().get();
    setNamespace(backup);
    backup.getStatus().getProcess().setStatus(BackupStatus.COMPLETED.status());
    backup.getStatus().setInternalName("test");
    backup.getStatus().setBackupInformation(new StackGresBackupInformation());
    backup.getStatus().getBackupInformation().setPostgresVersion(
        toNumericPostgresVersion(cluster.getSpec().getPostgres().getVersion()));
    minioSecret = Fixtures.secret().loadMinio().get();
    when(configScanner.findResources()).thenReturn(Optional.of(List.of(config)));
  }

  private void setNamespace(HasMetadata resource) {
    resource.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
  }

  @Test
  void givenValidCluster_shouldPass() {
    mockBackupConfig();
    mockPgConfig();
    mockPoolingConfig();
    mockProfile();
    when(backupFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backup));
    mockSecrets();

    generator.getRequiredResources(cluster);
  }

  private void mockProfile() {
    when(profileFinder.findByNameAndNamespace(
        cluster.getSpec().getSgInstanceProfile(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(instanceProfile));
  }

  private void mockPoolingConfig() {
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPoolingConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(poolingConfig));
  }

  private void mockPgConfig() {
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getSgPostgresConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.postgresConfig));
  }

  private void mockBackupConfig() {
    when(objectStorageFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.objectStorage));
  }

  private void mockSecrets() {
    when(secretFinder.findByNameAndNamespace(
        "minio",
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(minioSecret));
  }

}
