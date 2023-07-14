/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.PoolingConfigFinder;
import io.stackgres.common.resource.PostgresConfigFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

abstract class AbstractShardedClusterRequiredResourcesGeneratorTest {

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  PostgresConfigFinder postgresConfigFinder;

  @InjectMock
  ProfileConfigFinder profileConfigFinder;

  @InjectMock
  PoolingConfigFinder poolingConfigFinder;

  @InjectMock
  SecretFinder secretFinder;

  @Inject
  ShardedClusterRequiredResourcesGenerator generator;

  StackGresShardedCluster cluster;
  StackGresPostgresConfig postgresConfig;
  StackGresPoolingConfig poolingConfig;
  StackGresProfile instanceProfile;

  @BeforeEach
  void setUp() {
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
    instanceProfile = Fixtures.instanceProfile().loadSizeS().get();
    instanceProfile.getMetadata().setNamespace(namespace);
    setNamespace(instanceProfile);
  }

  private void setNamespace(HasMetadata resource) {
    resource.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
  }

  void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(cluster));
    assertEquals(message, ex.getMessage());
  }

  void mockProfile() {
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getResourceProfile(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(instanceProfile));
  }

  void mockPoolingConfig() {
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getConnectionPoolingConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(poolingConfig));
  }

  void mockPgConfig() {
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(this.postgresConfig));
  }

}
