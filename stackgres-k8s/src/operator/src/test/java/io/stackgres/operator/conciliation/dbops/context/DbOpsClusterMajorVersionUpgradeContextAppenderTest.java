/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsClusterMajorVersionUpgradeContextAppenderTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().toList();
  private static final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      ALL_SUPPORTED_POSTGRES_VERSIONS =
      ImmutableMap.of(
          StackGresComponent.POSTGRESQL, ImmutableMap.of(
              StackGresVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions())
              .append(SUPPORTED_POSTGRES_VERSIONS)
              .toList()));
  private static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();

  private DbOpsClusterMajorVersionUpgradeContextAppender contextAppender;

  private StackGresDbOps dbOps;

  private StackGresCluster cluster;

  private StackGresPostgresConfig postgresConfig;

  @Spy
  private StackGresDbOpsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.dbOps().loadMajorVersionUpgrade().get();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    cluster.getStatus().setPostgresVersion(SECOND_PG_MAJOR_VERSION);
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    postgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);
    contextAppender = new DbOpsClusterMajorVersionUpgradeContextAppender(
        postgresConfigFinder,
        ALL_SUPPORTED_POSTGRES_VERSIONS);
  }

  @Test
  void givenValidVersion_shouldPass() throws ValidationFailed {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setPostgresVersion(FIRST_PG_MINOR_VERSION);
    cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(
        new StackGresClusterDbOpsMajorVersionUpgradeStatus());
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
    cluster.getStatus().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("postgres version must be a newer major version than the current one (13 <= 13)", ex.getMessage());
  }

  @Test
  void givenInvalidVersion_shouldFail() {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MINOR_VERSION);
    cluster.getStatus().setPostgresVersion(SECOND_PG_MINOR_VERSION);

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals("postgres version must be a newer major version than the current one (13 <= 13)", ex.getMessage());
  }

  @Test
  void givenInvalidMajorVersion_shouldFail() {
    dbOps.getSpec().getMajorVersionUpgrade().setPostgresVersion(SECOND_PG_MAJOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.getStatus().setPostgresVersion(FIRST_PG_MINOR_VERSION);

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

  @Test
  void givenManagedCluster_shouldFail() {
    cluster.getMetadata().setOwnerReferences(List.of(
        new OwnerReferenceBuilder()
        .withKind("SGShardedCluster")
        .withName("test")
        .withController(true)
        .build()));

    var ex =
        assertThrows(IllegalArgumentException.class,
            () -> contextAppender.appendContext(dbOps, cluster, contextBuilder));
    assertEquals(
        "Can not perform major version upgrade on SGCluster managed by SGShardedCluster test",
        ex.getMessage());
  }

}
