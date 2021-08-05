/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
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

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions().findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedBuildVersions().findFirst().get();

  private StackGresClusterReview review;

  @Mock
  private ClusterExtensionMetadataManager extensionMetadataManager;

  private ExtensionsMutator mutator;

  private List<StackGresClusterInstalledExtension> defaultExtensions;

  @BeforeEach
  void setUp() throws Exception {
    review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    mutator = new ExtensionsMutator(extensionMetadataManager, JsonUtil.JSON_MAPPER);

    defaultExtensions = Seq.of(
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getDefaultExtension)
        .collect(ImmutableList.toImmutableList());
    when(extensionMetadataManager.getExtensionCandidateAnyVersion(
        same(review.getRequest().getObject()), any()))
        .then(this::getDefaultExtensionMetadata);
  }

  private StackGresExtensionMetadata getDefaultExtensionMetadata(InvocationOnMock invocation) {
    return new StackGresExtensionMetadata(defaultExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((StackGresClusterExtension) invocation.getArgument(1)).getName()))
        .findAny().get());
  }

  @Test
  void clusterWithoutExtensions_shouldNotDoAnything() {
    review.getRequest().getObject().getSpec().setPostgresExtensions(null);
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());
  }

  @Test
  void clusterWithoutExtensionsAndState_shouldCreateTheStateWithDefaultExtensions() {
    review.getRequest().getObject().getSpec().setPostgresExtensions(null);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());
  }

  @Test
  void clusterWithAnExtension_shouldSetTheVersionAndToInstall() throws Exception {
    StackGresClusterExtension extension = getExtension();
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);

    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(getExtensionMetadata());

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(2, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());
    assertEquals(1, operations.stream().filter(o -> o instanceof AddOperation).count());
  }

  @Test
  void clusterWithAnExtensionAlreadyInstalled_shouldNotDoAnything() throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedExtension);

    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(getExtensionMetadata());

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());
  }

  @Test
  void clusterWithExtensionInstalledAddADifferntExtension_shouldAddToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension, testExtension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(testExtension)))
        .thenReturn(extensionTestMetadata);
    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(extensionMetadata);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
  }

  @Test
  void clusterWithExtensionInstalledButRemoved_shouldReplaceToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(null);
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedExtension);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithExtensionInstalledAddDifferntExtension_shouldReplaceToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(getExtensionMetadata());

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithTwoExtensionInstalledAddDifferntExtension_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);
    final StackGresClusterInstalledExtension installedTestExtension2 = getInstalledExtension();
    installedTestExtension2.setName("test2");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension2);

    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(getExtensionMetadata());

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithExtensionInstalledAddExtensionWithExtraMounts_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(ImmutableList.of("test"));
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(extensionMetadata);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithExtensionInstalledWithExtraMountsAndExtension_shouldReplaceToInstallExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(ImmutableList.of("test"));
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(extensionMetadata);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithExtensionInstalledWithExtraMountsAddSimilarExtension_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    installedTestExtension.setExtraMounts(ImmutableList.of("test"));
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getVersion().setExtraMounts(ImmutableList.of("test"));
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(extensionMetadata);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithExtensionInstalledWithNoBuildAddDifferntExtension_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    installedTestExtension.setBuild(null);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(extensionMetadata);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithExtensionInstalledAddDifferntExtensionWithoutBuild_shouldReplaceToInstall()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    final StackGresClusterExtension extension = getExtension();
    extension.setVersion(installedExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(extension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionMetadata = getExtensionMetadata();
    extensionMetadata.getTarget().setBuild(null);
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(extension)))
        .thenReturn(extensionMetadata);

    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  @Test
  void clusterWithMissingExtension_shouldNotDoAnything() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(testExtension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(testExtension)))
        .thenThrow(IllegalArgumentException.class);
    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(0, operations.size());
  }

  @Test
  void clusterWithAnAlreadyInstalledMissingExtension_shouldReplaceToInstall() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension = getInstalledExtension();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    review.getRequest().getObject().getSpec().setPostgresExtensions(
        ImmutableList.of(testExtension));
    review.getRequest().getObject().getSpec().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .addAll(defaultExtensions);
    review.getRequest().getObject().getSpec().getToInstallPostgresExtensions()
        .add(installedTestExtension);

    final StackGresExtensionMetadata extensionTestMetadata = getExtensionMetadata();
    extensionTestMetadata.getExtension().setName("test");
    when(extensionMetadataManager.getExtensionCandidateSameMajorBuild(
        same(review.getRequest().getObject()), same(testExtension)))
        .thenThrow(IllegalArgumentException.class);
    final List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(1, operations.size());
    assertEquals(1, operations.stream().filter(o -> o instanceof ReplaceOperation).count());
  }

  private StackGresClusterExtension getExtension() {
    final StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName("timescaledb");
    return extension;
  }

  private StackGresClusterInstalledExtension getDefaultExtension(String name) {
    final StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName(name);
    installedExtension.setPublisher("com.ongres");
    installedExtension.setRepository(OperatorProperty.EXTENSIONS_REPOSITORY_URLS.getString());
    installedExtension.setVersion("1.0.0");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    installedExtension.setBuild(BUILD_VERSION);
    return installedExtension;
  }

  private StackGresClusterInstalledExtension getInstalledExtension() {
    final StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName("timescaledb");
    installedExtension.setPublisher("com.ongres");
    installedExtension.setRepository(OperatorProperty.EXTENSIONS_REPOSITORY_URLS.getString());
    installedExtension.setVersion("1.7.1");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    installedExtension.setBuild(BUILD_VERSION);
    return installedExtension;
  }

  private StackGresExtensionMetadata getExtensionMetadata() {
    return new StackGresExtensionMetadata(getInstalledExtension());
  }

}
