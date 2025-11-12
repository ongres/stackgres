/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterPostgresVersionContextAppenderTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .toList();
  private static final List<String> SUPPORTED_BABELFISH_VERSIONS =
      StackGresComponent.BABELFISH.getLatest().streamOrderedVersions().toList();
  private static final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      ALL_SUPPORTED_POSTGRES_VERSIONS =
      ImmutableMap.of(
          StackGresComponent.POSTGRESQL, ImmutableMap.of(
              StackGresVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions())
              .append(SUPPORTED_POSTGRES_VERSIONS)
              .toList()),
          StackGresComponent.BABELFISH, ImmutableMap.of(
              StackGresVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.BABELFISH.getLatest().streamOrderedMajorVersions())
              .append(SUPPORTED_BABELFISH_VERSIONS)
              .toList()));
  private static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .get(0).get();
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  private ShardedClusterPostgresVersionContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private EventEmitter<StackGresShardedCluster> eventController;

  @Mock
  private ShardedClusterCoordinatorPostgresConfigContextAppender clusterCoordinatorPostgresConfigContextAppender;

  @Mock
  private ShardedClusterShardsPostgresConfigContextAppender clusterShardsPostgresConfigContextAppender;

  @Mock
  private ShardedClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender;

  @Mock
  private ShardedClusterExtensionsContextAppender clusterExtensionsContextAppender;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.getStatus().setPostgresVersion(null);
    contextAppender = new ShardedClusterPostgresVersionContextAppender(
        eventController,
        clusterCoordinatorPostgresConfigContextAppender,
        clusterShardsPostgresConfigContextAppender,
        clusterRestoreBackupContextAppender,
        clusterExtensionsContextAppender,
        ALL_SUPPORTED_POSTGRES_VERSIONS);
  }

  @Test
  void clusterWithFinalPostgresVersion_shouldSetIt() {
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        cluster.getSpec().getPostgres().getVersion(),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty());
  }

  @Test
  void clusteWithNoPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    cluster.getSpec().getPostgres().setVersion(null);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        StackGresComponent.POSTGRESQL.getLatest().getLatestVersion(),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty());
  }

  @Test
  void clusteWithLatestPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.LATEST);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        StackGresComponent.POSTGRESQL.getLatest().getLatestVersion(),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty());
  }

  @Test
  void clusteWithMajorPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    cluster.getSpec().getPostgres().setVersion(
        StackGresComponent.POSTGRESQL.getLatest().getLatestMajorVersion());

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        StackGresComponent.POSTGRESQL.getLatest().getVersion(
            StackGresComponent.POSTGRESQL.getLatest().getLatestVersion()),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty());
  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {
    final String randomVersion = getRandomPostgresVersion();
    cluster.getSpec().getPostgres().setVersion(randomVersion);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        randomVersion,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, randomVersion);
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, randomVersion);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, randomVersion);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        randomVersion,
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty());
  }

  @Test
  void givenValidMajorPostgresVersion_shouldNotFail() throws ValidationFailed {
    final String randomMajorPostgresVersion = getMajorPostgresVersion(getRandomPostgresVersion());
    cluster.getSpec().getPostgres().setVersion(randomMajorPostgresVersion);

    contextAppender.appendContext(cluster, contextBuilder);

    assertNotNull(
        cluster.getStatus().getPostgresVersion());
    assertTrue(
        cluster.getStatus().getPostgresVersion().startsWith(randomMajorPostgresVersion + "."));
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), startsWith(randomMajorPostgresVersion + "."));
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), startsWith(randomMajorPostgresVersion + "."));
    verify(clusterRestoreBackupContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), startsWith(randomMajorPostgresVersion + "."));
    verify(clusterExtensionsContextAppender).appendContext(
        eq(cluster), eq(contextBuilder),
        startsWith(randomMajorPostgresVersion + "."),
        eq(cluster.getStatus().getBuildVersion()),
        eq(Optional.empty()),
        eq(Optional.empty()));
  }

  @Test
  void givenValidLatestPostgresVersion_shouldNotFail() throws ValidationFailed {
    final String latestVersion = getLatestPostgresVersion();
    cluster.getSpec().getPostgres().setVersion(latestVersion);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        latestVersion,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, latestVersion);
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, latestVersion);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, latestVersion);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        latestVersion,
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty());
  }

  @Test
  void givenInvalidPostgresVersion_shouldFail() {
    String invalidPostgresVersion = getRandomInvalidPostgresVersion();
    cluster.getSpec().getPostgres().setVersion(invalidPostgresVersion);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      contextAppender.appendContext(cluster, contextBuilder);
    });

    assertTrue(
        exception.getMessage().contains("Unsupported postgres version " + invalidPostgresVersion),
        exception.getMessage());
    verify(clusterCoordinatorPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void givenSamePostgresVersionUpdate_shouldNotFail() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    cluster.getStatus().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        FIRST_PG_MINOR_VERSION,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldNotChangeStatusVersion() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    final String previousVersion = getLatestPostgresVersionForMajorVersion(SECOND_PG_MAJOR_VERSION);
    cluster.getStatus().setPostgresVersion(previousVersion);
    final String previousBuild = "test";
    cluster.getStatus().setBuildVersion(previousBuild);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        previousVersion,
        cluster.getStatus().getPostgresVersion());
    assertEquals(
        previousBuild,
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void givenMajorPostgresVersionUpdateToAPreviousOne_shouldFail() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    final String previousVersion = getLatestPostgresVersionForMajorVersion(FIRST_PG_MAJOR_VERSION);
    cluster.getStatus().setPostgresVersion(previousVersion);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      contextAppender.appendContext(cluster, contextBuilder);
    });

    assertEquals(
        "Can not change the major version "
            + SECOND_PG_MAJOR_VERSION
            + " of Postgres to the previous major version "
            + FIRST_PG_MAJOR_VERSION,
        exception.getMessage());
    verify(clusterCoordinatorPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void givenMajorPostgresVersionUpdateWithMajorVersionUpdateDbOps_shouldChangeStatusVersion() throws ValidationFailed {
    final String targetVersion = getLatestPostgresVersionForMajorVersion(SECOND_PG_MAJOR_VERSION);
    cluster.getSpec().getPostgres().setVersion(targetVersion);
    cluster.setStatus(new StackGresShardedClusterStatus());
    final String previousVersion = getLatestPostgresVersionForMajorVersion(SECOND_PG_MAJOR_VERSION);
    cluster.getStatus().setPostgresVersion(previousVersion);
    final String buildVersion = "test";
    cluster.getStatus().setBuildVersion(buildVersion);
    cluster.getStatus().setDbOps(new StackGresShardedClusterDbOpsStatus());
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(new StackGresShardedClusterDbOpsMajorVersionUpgradeStatus());

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        targetVersion,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, targetVersion);
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, targetVersion);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, targetVersion);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        targetVersion,
        cluster.getStatus().getBuildVersion(),
        Optional.of(previousVersion),
        Optional.of(buildVersion));
  }

  @Test
  void givenMinorPostgresVersionUpdate_shouldPass() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    cluster.getStatus().setPostgresVersion(SECOND_PG_MINOR_VERSION);
    final String buildVersion = "test";
    cluster.getStatus().setBuildVersion(buildVersion);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        FIRST_PG_MINOR_VERSION,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterCoordinatorPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterShardsPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        FIRST_PG_MINOR_VERSION,
        cluster.getStatus().getBuildVersion(),
        Optional.of(SECOND_PG_MINOR_VERSION),
        Optional.of(buildVersion));
  }

  @Test
  void givenBuggyPostgresVersion_shouldFail() {
    String postgresVersion = getRandomBuggyPostgresVersion();
    cluster.getSpec().getPostgres().setVersion(postgresVersion);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      contextAppender.appendContext(cluster, contextBuilder);
    });

    assertTrue(
        exception.getMessage().contains("Do not use PostgreSQL " + postgresVersion),
        exception.getMessage());
    verify(clusterCoordinatorPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any());
  }

  private static String getRandomPostgresVersion() {
    Random random = new Random();
    List<String> validPostgresVersions = SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(ShardedClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains))
        .toList();

    int versionIndex = random.nextInt(validPostgresVersions.size());
    return validPostgresVersions.get(versionIndex);
  }

  private static String getLatestPostgresVersion() {
    return SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(ShardedClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains))
        .findFirst()
        .get();
  }

  private static String getLatestPostgresVersionForMajorVersion(String majorVersion) {
    return SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(ShardedClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains))
        .filter(version -> version.startsWith(majorVersion + "."))
        .findFirst()
        .get();
  }

  private static String getMajorPostgresVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  private static boolean isPostgresVersionValid(String version) {
    return SUPPORTED_POSTGRES_VERSIONS.stream().anyMatch(version::equals);
  }

  private static String getRandomInvalidPostgresVersion() {
    String version;

    Random random = new Random();
    do {

      Stream<String> versionDigits = random.ints(1, 100)
          .limit(2).mapToObj(i -> Integer.valueOf(i).toString());

      version = String.join(".", versionDigits.collect(Collectors.toList()));

    } while (isPostgresVersionValid(version));

    return version;
  }

  private static String getRandomBuggyPostgresVersion() {
    Random random = new Random();
    List<String> validBuggyPostgresVersions = ShardedClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()
        .stream()
        .filter(ShardedClusterPostgresVersionContextAppenderTest::isPostgresVersionValid)
        .toList();
    return validBuggyPostgresVersions.stream().toList()
        .get(random.nextInt(validBuggyPostgresVersions.size()));
  }

}
