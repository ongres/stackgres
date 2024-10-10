/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import static io.stackgres.common.docir.StackGresContextMock.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsClusterMajorVersionUpgradeContextAppenderTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(CONTEXT).toList();
  private static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedMajorVersions(CONTEXT)
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedMajorVersions(CONTEXT)
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(CONTEXT)
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(CONTEXT)
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();

  private ShardedDbOpsClusterMajorVersionUpgradeContextAppender contextAppender;

  private StackGresShardedDbOps dbOps;

  private StackGresShardedCluster cluster;

  private StackGresPostgresConfig postgresConfig;

  @Spy
  private StackGresShardedDbOpsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.shardedDbOps().loadMajorVersionUpgrade().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(
        StackGresKeys.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    postgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);
    contextAppender = new ShardedDbOpsClusterMajorVersionUpgradeContextAppender(
        StackGresContextMock.CONTEXT,
        postgresConfigFinder);
  }

  @Test
  void givenValidVersion_shouldPass() throws ValidationFailed {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    cluster.getStatus().setDbOps(new StackGresShardedClusterDbOpsStatus());
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(
        new StackGresShardedClusterDbOpsMajorVersionUpgradeStatus());
    cluster.getStatus().getDbOps().getMajorVersionUpgrade().setSourcePostgresVersion(
        SECOND_PG_MAJOR_VERSION);

    when(postgresConfigFinder.findByNameAndNamespace(
        dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(postgresConfig));

    contextAppender.appendContext(dbOps, cluster, contextBuilder);
  }

  @Test
  void givenValidVersionFromStatus_shouldPass() throws ValidationFailed {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    when(postgresConfigFinder.findByNameAndNamespace(
        dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(postgresConfig));

    contextAppender.appendContext(dbOps, cluster, contextBuilder);
  }

  @Test
  void givenSameVersion_shouldFail() {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("postgres version must be a newer major version than the current one (13 <= 13)", ex.getMessage());
  }

  @Test
  void givenInvalidVersion_shouldFail() {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MINOR_VERSION);

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("postgres version must be a newer major version than the current one (13 <= 13)", ex.getMessage());
  }

  @Test
  void givenInvalidMajorVersion_shouldFail() {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(SECOND_PG_MAJOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("postgres version must be a newer major version than the current one (12 <= 13)", ex.getMessage());
  }

  @Test
  void givenValidVersionButWrongConfig_shouldFail() throws ValidationFailed {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);

    postgresConfig.getSpec().setPostgresVersion(SECOND_PG_MAJOR_VERSION);

    when(postgresConfigFinder.findByNameAndNamespace(
        dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(postgresConfig));

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("SGPostgresConfig must be for postgres version 13 but was for version 12", ex.getMessage());
  }

  @Test
  void givenValidVersionButMissingConfig_shouldFail() throws ValidationFailed {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("SGPostgresConfig pg12 not found", ex.getMessage());
  }

}
