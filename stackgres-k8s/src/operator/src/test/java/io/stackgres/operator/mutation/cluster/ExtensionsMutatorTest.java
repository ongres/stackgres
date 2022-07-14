/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;
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
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions().findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedBuildVersions().findFirst().get();

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions()
          .toList();
  private static final List<String> SUPPORTED_BABELFISH_VERSIONS =
      StackGresComponent.BABELFISH.getLatest().getOrderedVersions().toList();
  private static final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
    ALL_SUPPORTED_POSTGRES_VERSIONS =
    ImmutableMap.of(
        StackGresComponent.POSTGRESQL, ImmutableMap.of(
            StackGresVersion.LATEST,
            Seq.of(StackGresComponent.LATEST)
            .append(StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions())
            .append(SUPPORTED_POSTGRES_VERSIONS)
            .collect(ImmutableList.toImmutableList())),
        StackGresComponent.BABELFISH, ImmutableMap.of(
            StackGresVersion.LATEST,
            Seq.of(StackGresComponent.LATEST)
            .append(StackGresComponent.BABELFISH.getLatest().getOrderedMajorVersions())
            .append(SUPPORTED_BABELFISH_VERSIONS)
            .collect(ImmutableList.toImmutableList())));

  private StackGresClusterReview review;

  @Mock
  private ClusterExtensionMetadataManager extensionMetadataManager;

  private ExtensionsMutator mutator;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> existingExtensions;

  private List<StackGresClusterInstalledExtension> toInstallExtensions;

  @BeforeEach
  void setUp() throws Exception {
    review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    mutator = new ExtensionsMutator(extensionMetadataManager, JsonUtil.JSON_MAPPER,
        ALL_SUPPORTED_POSTGRES_VERSIONS);

    extensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getExtension)
        .collect(ImmutableList.toImmutableList());
    existingExtensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getInstalledExtension)
        .collect(ImmutableList.toImmutableList());
    toInstallExtensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getInstalledExtensionWithoutBuild)
        .collect(ImmutableList.toImmutableList());
    lenient().when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), any(), anyBoolean()))
        .then(this::getDefaultExtensionMetadata);
  }

  private Optional<StackGresExtensionMetadata> getDefaultExtensionMetadata(
      InvocationOnMock invocation) {
    return Optional.of(new StackGresExtensionMetadata(existingExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((StackGresClusterExtension) invocation.getArgument(1)).getName()))
        .findAny().get()));
  }

  @Test
  void clusterWithoutExtensions_shouldNotDoAnything() {
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(extensions);
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(0, operations.size());
  }

  @Test
  void clusterWithIncorrectVersion_shouldNotDoAnything() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion("test");

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(0, operations.size());
  }

  @Test
  void clusterWithoutExtensionsAndState_shouldCreateTheStateWithDefaultExtensions() {
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(extensions);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());
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
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());
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
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(0, operations.size());
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
        same(review.getRequest().getObject()), same(testExtension), anyBoolean()))
        .thenReturn(Optional.of(extensionTestMetadata));
    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
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

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
    extensionMetadata.getVersion().setExtraMounts(ImmutableList.of("test"));
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
    installedTestExtension.setExtraMounts(ImmutableList.of("test"));
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
    installedTestExtension.setExtraMounts(ImmutableList.of("test"));
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(ImmutableList.of("test"));
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
        same(review.getRequest().getObject()), same(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithMissingExtension_shouldNotDoAnything() throws Exception {
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
        same(review.getRequest().getObject()), same(testExtension), anyBoolean()))
        .thenReturn(Optional.empty());
    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(0, operations.size());
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
        same(review.getRequest().getObject()), same(testExtension), anyBoolean()))
        .thenReturn(Optional.empty());
    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
