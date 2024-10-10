/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterExtensionsContextAppenderTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(StackGresContextMock.CONTEXT)
      .findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedMajorVersions(StackGresContextMock.CONTEXT)
      .findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedBuildVersions(StackGresContextMock.CONTEXT)
      .findFirst().get();

  private ClusterExtensionsContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private ExtensionMetadataManager extensionMetadataManager;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> existingExtensions;

  private List<StackGresClusterInstalledExtension> toInstallExtensions;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(
        StackGresKeys.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    disableRegistry(cluster);
    contextAppender = new ClusterExtensionsContextAppender(
        StackGresContextMock.CONTEXT,
        extensionMetadataManager);

    extensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getExtension)
        .toList();
    existingExtensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getInstalledExtension)
        .toList();
    toInstallExtensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getInstalledExtensionWithoutBuild)
        .toList();
    lenient().when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), argThat(extensions::contains), anyBoolean()))
        .then(this::getDefaultExtensionMetadata);
  }

  private Optional<StackGresExtensionMetadata> getDefaultExtensionMetadata(
      InvocationOnMock invocation) {
    if (invocation.getArgument(1) == null) {
      return Optional.empty();
    }
    return Optional.of(new StackGresExtensionMetadata(existingExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((StackGresClusterExtension) invocation.getArgument(1)).getName()))
        .findAny().get()));
  }

  @Test
  void clusterWithoutUserExtensions_shouldNotDoNothing() {
    cluster.getSpec().getPostgres().setExtensions(extensions);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);

    var expected = JsonUtil.copy(cluster);
    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(expected, cluster);
  }

  @Test
  void clusterWithoutExtensionsAndState_shouldCreateTheStateWithDefaultExtensions() {
    cluster.getSpec().getPostgres().setExtensions(extensions);

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(toInstallExtensions, cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithAnExtension_shouldSetToInstall() throws Exception {
    StackGresClusterExtension extension = getExtension();
    cluster.getSpec().getPostgres().setExtensions(
        Seq.seq(extensions).append(extension).toList());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    var expectedExtensions = JsonUtil.copy(cluster.getSpec().getPostgres()).getExtensions();

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        expectedExtensions,
        cluster.getSpec().getPostgres().getExtensions());
    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithAnExtensionAlreadyInstalled_shouldNotDoAnything() throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    cluster.getStatus().getExtensions()
        .add(installedExtension);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    var expected = JsonUtil.copy(cluster);
    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(expected, cluster);
  }

  @Test
  void clusterWithExtensionInstalledAddADifferntExtension_shouldAddToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).add(testExtension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(testExtension), anyBoolean()))
        .thenReturn(Optional.of(extensionTestMetadata));
    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions)
        .append(getInstalledExtensionWithoutBuild())
        .append(new StackGresClusterInstalledExtensionBuilder(getInstalledExtensionWithoutBuild())
            .withName("test")
            .build())
        .toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithExtensionInstalledButRemoved_shouldReplaceToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(extensions);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    cluster.getStatus().getExtensions()
        .add(installedExtension);

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    cluster.getSpec().getPostgres().getExtensions()
        .forEach(anExtension -> assertNotNull(anExtension.getVersion()));
    assertEquals(toInstallExtensions, cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithExtensionInstalledAddDifferntExtension_shouldReplaceToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithTwoExtensionInstalledAddDifferntExtension_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);
    final StackGresClusterInstalledExtension installedTestExtension2 =
        getInstalledExtensionWithoutBuild();
    installedTestExtension2.setName("test2");
    cluster.getStatus().getExtensions()
        .add(installedTestExtension2);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithExtensionInstalledAddExtensionWithExtraMounts_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(List.of("test"));
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions)
        .append(new StackGresClusterInstalledExtensionBuilder(getInstalledExtensionWithoutBuild())
            .withExtraMounts(List.of("test"))
            .build())
        .toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithExtensionInstalledWithExtraMountsAndExtension_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(List.of("test"));
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithExtensionInstalledWithExtraMountsAddSimilarExtension_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(List.of("test"));
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(List.of("test"));
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions)
        .append(new StackGresClusterInstalledExtensionBuilder(getInstalledExtensionWithoutBuild())
            .withExtraMounts(List.of("test"))
            .build())
        .toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithExtensionInstalledWithNoBuildAddDifferntExtension_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setBuild(null);
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithExtensionInstalledAddDifferntExtensionWithoutBuild_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getTarget().setBuild(null);
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        cluster.getStatus().getExtensions());
  }

  @Test
  void clusterWithMissingExtension_shouldFail() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(testExtension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(testExtension), anyBoolean()))
        .thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> contextAppender
        .appendContext(
            cluster,
            contextBuilder,
            POSTGRES_VERSION,
            BUILD_VERSION,
            Optional.empty(),
            Optional.empty(),
            cluster));
    assertEquals(
        "Extension was not found: test 1.7.1",
        exception.getMessage());
  }

  @Test
  void clusterWithAnAlreadyInstalledMissingExtension_shouldReplaceToInstall() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(testExtension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);
    cluster.getStatus().getExtensions()
        .add(installedTestExtension);
    toInstallExtensions.add(installedTestExtension);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(testExtension), anyBoolean()))
        .thenReturn(Optional.of(new StackGresExtensionMetadata(installedTestExtension)));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    cluster.getSpec().getPostgres().getExtensions()
        .forEach(anExtension -> assertNotNull(anExtension.getVersion()));
    assertEquals(toInstallExtensions, cluster.getStatus().getExtensions());
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    cluster.getSpec().getPostgres().setExtensions(extensions);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);
  }

  @Test
  void givenAnUpdate_shouldPass() throws ValidationFailed {
    cluster.getSpec().getPostgres().setExtensions(extensions);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions()
        .addAll(toInstallExtensions);

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.of(POSTGRES_VERSION),
        Optional.of(BUILD_VERSION),
        cluster);
  }

  @Test
  void givenACreationWithMissingExtensions_shouldFail() {
    cluster.getSpec().getPostgres().setExtensions(extensions);
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean())
    ).thenReturn(Optional.empty());
    when(extensionMetadataManager.getExtensionsAnyVersion(
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean())
    ).thenReturn(List.of());

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> contextAppender
        .appendContext(
            cluster,
            contextBuilder,
            POSTGRES_VERSION,
            BUILD_VERSION,
            Optional.empty(),
            Optional.empty(),
            cluster));
    assertEquals(
        "Some extensions were not found: dblink 1.0.0, pg_stat_statements 1.0.0, plpgsql 1.0.0, plpython3u 1.0.0",
        exception.getMessage());
  }

  @Test
  void givenLatestExtensionMatchesSpecVersion_shouldNotSetLatest() throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions().addAll(toInstallExtensions);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    final StackGresClusterInstalledExtension timescaledb = cluster.getStatus().getExtensions()
        .stream()
        .filter(installed -> "timescaledb".equals(installed.getName()))
        .findFirst().get();
    assertNull(timescaledb.getLatest());
  }

  @Test
  void givenLatestExtensionDiffersFromSpecVersion_shouldSetLatest() throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions().addAll(toInstallExtensions);

    // The spec-version-driven query resolves the installed version (1.7.1),
    // the no-version query returns a newer candidate (1.8.0) to advertise as `latest`.
    final StackGresExtensionMetadata pinnedMetadata = getExtensionMetadata();
    final StackGresExtensionMetadata latestMetadata = getExtensionMetadata();
    latestMetadata.getVersion().setVersion("1.8.0");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> anExtension != null
            && extension.getName().equals(anExtension.getName())
            && anExtension.getVersion() != null),
        anyBoolean()))
        .thenReturn(Optional.of(pinnedMetadata));
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> anExtension != null
            && extension.getName().equals(anExtension.getName())
            && anExtension.getVersion() == null),
        anyBoolean()))
        .thenReturn(Optional.of(latestMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    final StackGresClusterInstalledExtension timescaledb = cluster.getStatus().getExtensions()
        .stream()
        .filter(installed -> "timescaledb".equals(installed.getName()))
        .findFirst().get();
    assertEquals("1.8.0", timescaledb.getLatest());
  }

  @Test
  void givenSpecExtensionWithoutVersion_shouldNotSetLatest() throws Exception {
    final StackGresClusterExtension extension = getExtension();
    // no version set => uses the default channel; latest must not be advertised.
    cluster.getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setExtensions(new ArrayList<>());
    cluster.getStatus().getExtensions().addAll(toInstallExtensions);

    final StackGresExtensionMetadata latestMetadata = getExtensionMetadata();
    latestMetadata.getVersion().setVersion("1.8.0");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(latestMetadata));

    contextAppender.appendContext(
        cluster,
        contextBuilder,
        POSTGRES_VERSION,
        BUILD_VERSION,
        Optional.empty(),
        Optional.empty(),
        cluster);

    final StackGresClusterInstalledExtension timescaledb = cluster.getStatus().getExtensions()
        .stream()
        .filter(installed -> "timescaledb".equals(installed.getName()))
        .findFirst().get();
    assertNull(timescaledb.getLatest());
  }

  private StackGresClusterExtension getExtension() {
    final StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName("timescaledb");
    return extension;
  }

  private StackGresClusterExtension getExtension(String name) {
    final StackGresClusterExtension extension =
        new StackGresClusterExtension();
    extension.setName(name);
    extension.setVersion("1.0.0");
    return extension;
  }

  private StackGresClusterInstalledExtension getInstalledExtension(String name) {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild(name);
    installedExtension.setBuild(BUILD_VERSION);
    return installedExtension;
  }

  private StackGresClusterInstalledExtension getInstalledExtension() {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    installedExtension.setBuild(BUILD_VERSION);
    return installedExtension;
  }

  private StackGresClusterInstalledExtension getInstalledExtensionWithoutBuild(String name) {
    final StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName(name);
    installedExtension.setPublisher("com.ongres");
    installedExtension.setRepository(OperatorProperty.EXTENSIONS_REPOSITORY_URLS.getString());
    installedExtension.setVersion("1.0.0");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    return installedExtension;
  }

  private StackGresClusterInstalledExtension getInstalledExtensionWithoutBuild() {
    final StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName("timescaledb");
    installedExtension.setPublisher("com.ongres");
    installedExtension.setRepository(OperatorProperty.EXTENSIONS_REPOSITORY_URLS.getString());
    installedExtension.setVersion("1.7.1");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    return installedExtension;
  }

  private StackGresExtensionMetadata getExtensionMetadata() {
    return new StackGresExtensionMetadata(getInstalledExtension());
  }

  private static void disableRegistry(StackGresCluster cluster) {
    if (cluster.getSpec().getConfigurations() == null) {
      cluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
    }
    cluster.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    cluster.getSpec().getConfigurations().getRegistry().setEnabled(false);
  }

}
