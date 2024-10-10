/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.fge.jsonpatch.JsonPatchException;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusAddon;
import io.stackgres.common.docir.DocirAddon;
import io.stackgres.common.docir.DocirMetadataManager;
import io.stackgres.common.docir.DocirUtil;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterPostgresVersionContextAppenderTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(StackGresContextMock.CONTEXT)
          .toList();
  private static final List<String> SUPPORTED_BABELFISH_VERSIONS =
      StackGresComponent.BABELFISH.get(Fixtures.registryCluster())
          .streamOrderedVersions(StackGresContextMock.CONTEXT).toList();
  private static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedMajorVersions(StackGresContextMock.CONTEXT)
          .get(0).get();
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedMajorVersions(StackGresContextMock.CONTEXT)
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(StackGresContextMock.CONTEXT)
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(StackGresContextMock.CONTEXT)
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(StackGresContextMock.CONTEXT)
      .findFirst().get();

  private ClusterPostgresVersionContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private EventEmitter<StackGresCluster> eventController;

  @Mock
  private ClusterPostgresConfigContextAppender clusterPostgresConfigContextAppender;

  @Mock
  private ClusterDefaultBackupPathContextAppender clusterDefaultBackupPathContextAppender;

  @Mock
  private ClusterObjectStorageContextAppender clusterObjectStorageContextAppender;

  @Mock
  private ClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender;

  @Mock
  private ClusterExtensionsContextAppender clusterExtensionsContextAppender;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(StackGresKeys.ROLLOUT_KEY, StackGresKeys.ROLLOUT_ALWAYS_VALUE);
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.getStatus().setPostgresVersion(null);
    contextAppender = new ClusterPostgresVersionContextAppender(
        StackGresContextMock.CONTEXT,
        eventController,
        clusterPostgresConfigContextAppender,
        clusterDefaultBackupPathContextAppender,
        clusterRestoreBackupContextAppender,
        clusterObjectStorageContextAppender,
        clusterExtensionsContextAppender);
  }

  @Test
  void clusterWithFinalPostgresVersion_shouldSetIt() {
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        cluster.getSpec().getPostgres().getVersion(),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getMetadata().getAnnotations().get(StackGresKeys.VERSION_KEY));
    assertEquals(
        StackGresProperty.OPERATOR_VERSION.getString(),
        cluster.getMetadata().getAnnotations().get(StackGresKeys.VERSION_KEY));
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty(),
        cluster);
  }

  @Test
  void givenSidecarAddonMissingInRepository_shouldPinOnlyAvailableAddons() {
    contextAppender = createContextAppenderWithoutAddon(DocirUtil.OTEL_COLLECTOR_ADDON);
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    contextAppender.appendContext(cluster, contextBuilder);

    final StackGresClusterStatus status = cluster.getStatus();
    assertNotNull(status.getRepository());
    assertNotNull(status.getRevision());
    assertNotNull(status.getBase());
    assertNotNull(status.getBaseVersion());
    assertNotNull(status.getBaseRevision());
    for (String addon : DocirUtil.POSTGRES_IMAGE_ADDONS) {
      assertTrue(status.findAddon(addon).isPresent(), "Addon " + addon + " not pinned");
    }
    for (String addon : DocirUtil.SIDECAR_ADDONS.values()) {
      assertEquals(!DocirUtil.OTEL_COLLECTOR_ADDON.equals(addon),
          status.findAddon(addon).isPresent(), "Addon " + addon);
    }
  }

  @Test
  void givenSidecarAddonNotPinned_shouldPinItWhenAvailableWithoutChangingOtherPins() {
    contextAppender = createContextAppenderWithoutAddon(DocirUtil.OTEL_COLLECTOR_ADDON);
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    contextAppender.appendContext(cluster, contextBuilder);
    assertTrue(cluster.getStatus().findAddon(DocirUtil.OTEL_COLLECTOR_ADDON).isEmpty());
    final List<StackGresClusterStatusAddon> pinnedAddons =
        List.copyOf(cluster.getStatus().getAddons());
    final String pinnedRevision = cluster.getStatus().getRevision();
    cluster.getMetadata().getAnnotations().remove(StackGresKeys.ROLLOUT_KEY);
    contextAppender = new ClusterPostgresVersionContextAppender(
        StackGresContextMock.CONTEXT,
        eventController,
        clusterPostgresConfigContextAppender,
        clusterDefaultBackupPathContextAppender,
        clusterRestoreBackupContextAppender,
        clusterObjectStorageContextAppender,
        clusterExtensionsContextAppender);

    contextAppender.appendContext(cluster, contextBuilder);

    final StackGresClusterStatus status = cluster.getStatus();
    assertEquals(pinnedRevision, status.getRevision());
    assertTrue(status.findAddon(DocirUtil.OTEL_COLLECTOR_ADDON).isPresent());
    assertEquals(pinnedAddons.size() + 1, status.getAddons().size());
    assertTrue(status.getAddons().containsAll(pinnedAddons));
  }

  private ClusterPostgresVersionContextAppender createContextAppenderWithoutAddon(
      String missingAddon) {
    final DocirMetadataManager metadataManager =
        spy(StackGresContextMock.CONTEXT.getMetadataManager());
    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      final List<DocirAddon> addons = (List<DocirAddon>) invocation.callRealMethod();
      return addons.stream()
          .filter(addon -> !addon.getName().equals(missingAddon))
          .toList();
    }).when(metadataManager).getAddons(nullable(URI.class));
    final StackGresContext context = spy(StackGresContextMock.CONTEXT);
    doReturn(metadataManager).when(context).getMetadataManager();
    return new ClusterPostgresVersionContextAppender(
        context,
        eventController,
        clusterPostgresConfigContextAppender,
        clusterDefaultBackupPathContextAppender,
        clusterRestoreBackupContextAppender,
        clusterObjectStorageContextAppender,
        clusterExtensionsContextAppender);
  }

  @Test
  void clusteWithNoPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    cluster.getSpec().getPostgres().setVersion(null);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).getLatestVersion(StackGresContextMock.CONTEXT),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty(),
        cluster);
  }

  @Test
  void clusteWithLatestPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.LATEST);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).getLatestVersion(StackGresContextMock.CONTEXT),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty(),
        cluster);
  }

  @Test
  void clusteWithMajorPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    cluster.getSpec().getPostgres().setVersion(
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .getLatestMajorVersion(StackGresContextMock.CONTEXT));

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).getVersion(
            StackGresContextMock.CONTEXT,
            StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
                .getLatestVersion(StackGresContextMock.CONTEXT)),
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, cluster.getStatus().getPostgresVersion());
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        cluster.getStatus().getPostgresVersion(),
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty(),
        cluster);
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
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, randomVersion);
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, randomVersion);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, randomVersion);
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, randomVersion);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        randomVersion,
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty(),
        cluster);
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
    verify(clusterPostgresConfigContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), startsWith(randomMajorPostgresVersion + "."));
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), startsWith(randomMajorPostgresVersion + "."));
    verify(clusterRestoreBackupContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), startsWith(randomMajorPostgresVersion + "."));
    verify(clusterObjectStorageContextAppender).appendContext(
        eq(cluster), eq(contextBuilder), startsWith(randomMajorPostgresVersion + "."));
    verify(clusterExtensionsContextAppender).appendContext(
        eq(cluster), eq(contextBuilder),
        startsWith(randomMajorPostgresVersion + "."),
        eq(cluster.getStatus().getBuildVersion()),
        eq(Optional.empty()),
        eq(Optional.empty()),
        eq(cluster));
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
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, latestVersion);
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, latestVersion);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, latestVersion);
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, latestVersion);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        latestVersion,
        cluster.getStatus().getBuildVersion(),
        Optional.empty(),
        Optional.empty(),
        cluster);
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
    verify(clusterPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterDefaultBackupPathContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterObjectStorageContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void givenSamePostgresVersionUpdate_shouldNotFail() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setPostgresVersion(FIRST_PG_MINOR_VERSION);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        FIRST_PG_MINOR_VERSION,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldNotChangeStatusVersion() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    cluster.setStatus(new StackGresClusterStatus());
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
    verify(clusterPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterDefaultBackupPathContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterObjectStorageContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void givenMajorPostgresVersionUpdateToAPreviousOne_shouldFail() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    cluster.setStatus(new StackGresClusterStatus());
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
    verify(clusterPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterDefaultBackupPathContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterObjectStorageContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void givenMajorPostgresVersionUpdateWithMajorVersionUpdateDbOps_shouldChangeStatusVersion() throws ValidationFailed {
    final String targetVersion = getLatestPostgresVersionForMajorVersion(SECOND_PG_MAJOR_VERSION);
    cluster.getSpec().getPostgres().setVersion(targetVersion);
    cluster.setStatus(new StackGresClusterStatus());
    final String previousVersion = getLatestPostgresVersionForMajorVersion(SECOND_PG_MAJOR_VERSION);
    cluster.getStatus().setPostgresVersion(previousVersion);
    final String buildVersion = "test";
    cluster.getStatus().setBuildVersion(buildVersion);
    cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(new StackGresClusterDbOpsMajorVersionUpgradeStatus());

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        targetVersion,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, targetVersion);
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, targetVersion);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, targetVersion);
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, targetVersion);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        targetVersion,
        cluster.getStatus().getBuildVersion(),
        Optional.of(previousVersion),
        Optional.of(buildVersion),
        cluster);
  }

  @Test
  void givenMinorPostgresVersionUpdate_shouldPass() throws ValidationFailed {
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setPostgresVersion(SECOND_PG_MINOR_VERSION);
    final String buildVersion = "test";
    cluster.getStatus().setBuildVersion(buildVersion);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(
        FIRST_PG_MINOR_VERSION,
        cluster.getStatus().getPostgresVersion());
    assertNotNull(
        cluster.getStatus().getBuildVersion());
    verify(clusterPostgresConfigContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterDefaultBackupPathContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterRestoreBackupContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterObjectStorageContextAppender).appendContext(
        cluster, contextBuilder, FIRST_PG_MINOR_VERSION);
    verify(clusterExtensionsContextAppender).appendContext(
        cluster, contextBuilder,
        FIRST_PG_MINOR_VERSION,
        cluster.getStatus().getBuildVersion(),
        Optional.of(SECOND_PG_MINOR_VERSION),
        Optional.of(buildVersion),
        cluster);
  }

  @Test
  void givenBuggyPostgresVersion_shouldFail() {
    Assumptions.assumeFalse(ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()
        .stream()
        .noneMatch(ClusterPostgresVersionContextAppenderTest::isPostgresVersionValid));
    String postgresVersion = getRandomBuggyPostgresVersion();
    cluster.getSpec().getPostgres().setVersion(postgresVersion);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      contextAppender.appendContext(cluster, contextBuilder);
    });

    assertTrue(
        exception.getMessage().contains("Do not use PostgreSQL " + postgresVersion),
        exception.getMessage());
    verify(clusterPostgresConfigContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterDefaultBackupPathContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterRestoreBackupContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterObjectStorageContextAppender, never()).appendContext(
        any(), any(), any());
    verify(clusterExtensionsContextAppender, never()).appendContext(
        any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void givenLatestPostgresVersion_shouldNotSetLatestStatusFields() {
    final String latestVersion = StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
        .getLatestVersion(StackGresContextMock.CONTEXT);
    cluster.getSpec().getPostgres().setVersion(latestVersion);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(latestVersion, cluster.getStatus().getPostgresVersion());
    assertNull(cluster.getStatus().getLatestPostgresMinor());
    assertNull(cluster.getStatus().getLatestPostgresMajor());
  }

  @Test
  void givenNotLatestMinorOfLatestMajor_shouldSetOnlyLatestPostgresMinor() {
    final String latestMajor =
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .getLatestMajorVersion(StackGresContextMock.CONTEXT);
    final String latestMinorOfLatestMajor =
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .getVersion(StackGresContextMock.CONTEXT, latestMajor);
    final String notLatestMinor =
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .streamOrderedVersions(StackGresContextMock.CONTEXT)
            .filter(version -> version.startsWith(latestMajor + "."))
            .filter(version -> !version.equals(latestMinorOfLatestMajor))
            .findFirst().get();
    cluster.getSpec().getPostgres().setVersion(notLatestMinor);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(notLatestMinor, cluster.getStatus().getPostgresVersion());
    assertEquals(latestMinorOfLatestMajor, cluster.getStatus().getLatestPostgresMinor());
    assertNull(cluster.getStatus().getLatestPostgresMajor());
  }

  @Test
  void givenLatestMinorOfPreviousMajor_shouldSetOnlyLatestPostgresMajor() {
    final String previousMajor = SECOND_PG_MAJOR_VERSION;
    final String latestMinorOfPreviousMajor =
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .getVersion(StackGresContextMock.CONTEXT, previousMajor);
    cluster.getSpec().getPostgres().setVersion(latestMinorOfPreviousMajor);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(latestMinorOfPreviousMajor, cluster.getStatus().getPostgresVersion());
    assertNull(cluster.getStatus().getLatestPostgresMinor());
    assertEquals(
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).getLatestVersion(StackGresContextMock.CONTEXT),
        cluster.getStatus().getLatestPostgresMajor());
  }

  @Test
  void givenNotLatestMinorOfPreviousMajor_shouldSetBothLatestStatusFields() {
    final String previousMajor = StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
        .streamOrderedMajorVersions(StackGresContextMock.CONTEXT)
        .skip(1)
        .filter(major -> StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .streamOrderedVersions(StackGresContextMock.CONTEXT)
            .filter(version -> version.startsWith(major + "."))
            .count() >= 2)
        .findFirst().get();
    final String latestMinorOfPreviousMajor =
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .getVersion(StackGresContextMock.CONTEXT, previousMajor);
    final String notLatestMinor =
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
            .streamOrderedVersions(StackGresContextMock.CONTEXT)
            .filter(version -> version.startsWith(previousMajor + "."))
            .filter(version -> !version.equals(latestMinorOfPreviousMajor))
            .findFirst().get();
    cluster.getSpec().getPostgres().setVersion(notLatestMinor);

    contextAppender.appendContext(cluster, contextBuilder);

    assertEquals(notLatestMinor, cluster.getStatus().getPostgresVersion());
    assertEquals(latestMinorOfPreviousMajor, cluster.getStatus().getLatestPostgresMinor());
    assertEquals(
        StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).getLatestVersion(StackGresContextMock.CONTEXT),
        cluster.getStatus().getLatestPostgresMajor());
  }

  private static String getRandomPostgresVersion() {
    Random random = new Random();
    List<String> validPostgresVersions = SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains))
        .toList();

    int versionIndex = random.nextInt(validPostgresVersions.size());
    return validPostgresVersions.get(versionIndex);
  }

  private static String getLatestPostgresVersion() {
    return SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains))
        .findFirst()
        .get();
  }

  private static String getLatestPostgresVersionForMajorVersion(String majorVersion) {
    return SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains))
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
    List<String> validBuggyPostgresVersions = ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()
        .stream()
        .filter(ClusterPostgresVersionContextAppenderTest::isPostgresVersionValid)
        .toList();
    return validBuggyPostgresVersions.stream().toList()
        .get(random.nextInt(validBuggyPostgresVersions.size()));
  }

}
