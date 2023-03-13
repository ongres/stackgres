/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorCluster;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.OperatorExtensionMetadataManager;
import io.stackgres.operator.common.StackGresShardedClusterReview;
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
  private static final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      ALL_SUPPORTED_POSTGRES_VERSIONS =
      ImmutableMap.of(
          StackGresComponent.POSTGRESQL, ImmutableMap.of(
              StackGresVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions())
              .append(SUPPORTED_POSTGRES_VERSIONS)
              .collect(ImmutableList.toImmutableList())));

  private StackGresShardedClusterReview review;

  @Mock
  private OperatorExtensionMetadataManager extensionMetadataManager;

  private ExtensionsMutator mutator;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> existingExtensions;

  private List<StackGresClusterInstalledExtension> toInstallExtensions;

  @BeforeEach
  void setUp() throws Exception {
    review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().setStatus(new StackGresShardedClusterStatus());

    mutator = new ExtensionsMutator(extensionMetadataManager, JsonUtil.jsonMapper(),
        ALL_SUPPORTED_POSTGRES_VERSIONS);

    extensions = List.of();
    existingExtensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getInstalledExtension)
        .collect(ImmutableList.toImmutableList());
    toInstallExtensions = Seq.of(
        "citus",
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getInstalledExtensionWithoutBuild)
        .collect(ImmutableList.toImmutableList());
    lenient().when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        same(getCoordinatorCluster(review.getRequest().getObject())),
            any(), anyBoolean()))
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
  void clusterWithoutUserExtensions_shouldAddDefaultExtensions() {
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(extensions);
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithIncorrectVersion_shouldNotDoNothing() {
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());
  }

  @Test
  void clusterWithAnExtensionAlreadyInstalled_shouldAddCitusExtension() throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedExtension);

    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(testExtension), anyBoolean()))
        .thenReturn(Optional.of(extensionTestMetadata));
    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);
    final StackGresClusterInstalledExtension installedTestExtension2 =
        getInstalledExtensionWithoutBuild();
    installedTestExtension2.setName("test2");
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension2);

    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(ImmutableList.of("test"));
    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(ImmutableList.of("test"));
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(ImmutableList.of("test"));
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(ImmutableList.of("test"));
    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    installedTestExtension.setBuild(null);
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getTarget().setBuild(null);
    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(extension), anyBoolean()))
        .thenReturn(Optional.of(extensionMetadata));

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithMissingExtension_shouldAddDefaultExtensions() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    review.getRequest().getObject().getSpec().getPostgres().setExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(testExtension).build());
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(testExtension), anyBoolean()))
        .thenReturn(Optional.empty());
    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
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
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    var coordinator = getCoordinatorCluster(review.getRequest().getObject());
    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        eq(coordinator), same(testExtension), anyBoolean()))
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
