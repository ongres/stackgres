/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ShardedClusterRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ProfileFinder profileFinder;

  @InjectMock
  PostgresConfigFinder postgresConfigFinder;

  @InjectMock
  PoolingConfigFinder poolingConfigFinder;

  @InjectMock
  SecretFinder secretFinder;

  @Inject
  ShardedClusterRequiredResourcesGenerator generator;

  StackGresConfig config;
  StackGresShardedCluster cluster;
  StackGresPostgresConfig postgresConfig;
  StackGresPoolingConfig poolingConfig;
  StackGresProfile instanceProfile;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().streamOrderedVersions()
        .skipWhile(version -> version.startsWith("15")).findFirst().orElseThrow());
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    final String namespace = cluster.getMetadata().getNamespace();
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
    when(configScanner.findResources()).thenReturn(Optional.of(List.of(config)));
  }

  private void setNamespace(HasMetadata resource) {
    resource.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
  }

  @Test
  void givenValidCluster_shouldPass() {
    mockProfile();
    mockPgConfig();
    mockPoolConfig();

    generator.getRequiredResources(cluster);
  }

  void mockProfile() {
    when(profileFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.instanceProfile));
    when(profileFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getSgInstanceProfile(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.instanceProfile));
  }

  void mockPgConfig() {
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.postgresConfig));
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.postgresConfig));
  }

  void mockPoolConfig() {
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.poolingConfig));
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.poolingConfig));
  }

}
