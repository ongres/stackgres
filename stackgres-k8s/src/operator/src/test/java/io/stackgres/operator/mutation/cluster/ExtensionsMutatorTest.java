/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtensionBuilder;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.OperatorExtensionMetadataManager;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtensionsMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions().findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedBuildVersions().findFirst().get();

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

  private StackGresClusterReview review;

  @Mock
  private OperatorExtensionMetadataManager extensionMetadataManager;

  private ExtensionsMutator mutator;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> existingExtensions;

  private List<StackGresClusterInstalledExtension> toInstallExtensions;

  @BeforeEach
  void setUp() throws Exception {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    mutator = new ExtensionsMutator(extensionMetadataManager,
        ALL_SUPPORTED_POSTGRES_VERSIONS);

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
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(extensions);
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusterWithIncorrectVersion_shouldNotDoNothing() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion("test");

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusterWithoutExtensionsAndState_shouldCreateTheStateWithDefaultExtensions() {
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(extensions);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(toInstallExtensions, result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithAnExtension_shouldSetTheVersionAndToInstall() throws Exception {
    StackGresClusterExtension extension = getExtension();
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    result.getSpec().getPostgres().getExtensions()
        .forEach(anExtension -> assertNotNull(anExtension.getVersion()));
    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithAnExtensionAlreadyInstalled_shouldNotDoAnything() throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedExtension);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
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
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).add(testExtension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
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

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions)
        .append(getInstalledExtensionWithoutBuild())
        .append(new StackGresClusterInstalledExtensionBuilder(getInstalledExtensionWithoutBuild())
            .withName("test")
            .build())
        .toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithExtensionInstalledButRemoved_shouldReplaceToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(extensions);
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedExtension);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    result.getSpec().getPostgres().getExtensions()
        .forEach(anExtension -> assertNotNull(anExtension.getVersion()));
    assertEquals(toInstallExtensions, result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithExtensionInstalledAddDifferntExtension_shouldReplaceToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithTwoExtensionInstalledAddDifferntExtension_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);
    final StackGresClusterInstalledExtension installedTestExtension2 =
        getInstalledExtensionWithoutBuild();
    installedTestExtension2.setName("test2");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension2);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithExtensionInstalledAddExtensionWithExtraMounts_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(List.of("test"));
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions)
        .append(new StackGresClusterInstalledExtensionBuilder(getInstalledExtensionWithoutBuild())
            .withExtraMounts(List.of("test"))
            .build())
        .toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithExtensionInstalledWithExtraMountsAndExtension_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(List.of("test"));
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithExtensionInstalledWithExtraMountsAddSimilarExtension_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(List.of("test"));
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(List.of("test"));
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions)
        .append(new StackGresClusterInstalledExtensionBuilder(getInstalledExtensionWithoutBuild())
            .withExtraMounts(List.of("test"))
            .build())
        .toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithExtensionInstalledWithNoBuildAddDifferntExtension_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setBuild(null);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithExtensionInstalledAddDifferntExtensionWithoutBuild_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getTarget().setBuild(null);
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        result.getSpec().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithMissingExtension_shouldNotDoNothing() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(testExtension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(testExtension), anyBoolean()))
        .thenReturn(Optional.empty());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusterWithAnAlreadyInstalledMissingExtension_shouldReplaceToInstall() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(testExtension).build());
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(testExtension), anyBoolean()))
        .thenReturn(Optional.empty());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    result.getSpec().getPostgres().getExtensions()
        .forEach(anExtension -> assertNotNull(anExtension.getVersion()));
    assertEquals(toInstallExtensions, result.getSpec().getToInstallPostgresExtensions());
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

}
