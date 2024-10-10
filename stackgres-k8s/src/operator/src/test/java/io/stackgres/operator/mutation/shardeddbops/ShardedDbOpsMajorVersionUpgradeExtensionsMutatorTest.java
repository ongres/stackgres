/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

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
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
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
class ShardedDbOpsMajorVersionUpgradeExtensionsMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedMajorVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedBuildVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()).streamOrderedVersions(StackGresContextMock.CONTEXT)
          .toList();
  private static final List<String> SUPPORTED_BABELFISH_VERSIONS =
      StackGresComponent.BABELFISH.get(Fixtures.registryCluster())
          .streamOrderedVersions(StackGresContextMock.CONTEXT).toList();
  private StackGresShardedDbOpsReview review;

  @Mock
  private ExtensionMetadataManager extensionMetadataManager;

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private ShardedDbOpsMajorVersionUpgradeExtensionsMutator mutator;

  private StackGresShardedCluster cluster;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> existingExtensions;

  private List<StackGresClusterInstalledExtension> toInstallExtensions;

  @BeforeEach
  void setUp() throws Exception {
    review = AdmissionReviewFixtures.shardedDbOps().loadMajorVersionUpgradeCreate().get();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresVersion(POSTGRES_VERSION);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(
        StackGresKeys.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    disableRegistry(cluster);

    mutator = new ShardedDbOpsMajorVersionUpgradeExtensionsMutator(StackGresContextMock.CONTEXT,
        extensionMetadataManager,
        clusterFinder);

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
    lenient().when(clusterFinder.findByNameAndNamespace(
        any(), any()))
        .thenReturn(Optional.of(cluster));
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
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresExtensions(extensions);
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    StackGresShardedDbOps result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusterWithIncorrectVersion_shouldNotDoNothing() {
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion("test");

    StackGresShardedDbOps result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusterWithoutExtensionsAndState_shouldCreateTheStateWithDefaultExtensions() {
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresExtensions(extensions);

    StackGresShardedDbOps result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(toInstallExtensions, result.getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithAnExtension_shouldSetTheVersionAndToInstall() throws Exception {
    StackGresClusterExtension extension = getExtension();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(extension).build());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(),
        argThat(anExtension -> extension.getName().equals(anExtension.getName())),
        anyBoolean()))
        .thenReturn(Optional.of(getExtensionMetadata()));

    StackGresShardedDbOps result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    result.getSpec().getMajorVersionUpgrade().getPostgresExtensions()
        .forEach(anExtension -> assertNotNull(anExtension.getVersion()));
    assertEquals(
        Seq.seq(toInstallExtensions).append(getInstalledExtensionWithoutBuild()).toList(),
        result.getSpec().getMajorVersionUpgrade().getToInstallPostgresExtensions());
  }

  @Test
  void clusterWithMissingExtension_shouldNotDoNothing() throws Exception {
    final StackGresClusterInstalledExtension installedTestExtension =
        getInstalledExtensionWithoutBuild();
    installedTestExtension.setName("test");
    final StackGresClusterExtension testExtension = getExtension();
    testExtension.setName("test");
    testExtension.setVersion(installedTestExtension.getVersion());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresExtensions(
        ImmutableList.<StackGresClusterExtension>builder()
        .addAll(extensions).add(testExtension).build());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);

    when(extensionMetadataManager.findExtensionCandidateSameMajorBuild(
        any(), eq(testExtension), anyBoolean()))
        .thenReturn(Optional.empty());

    StackGresShardedDbOps result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusterWithExtensionInstalledButRemoved_shouldReplaceToInstallPostgresExtensions()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension =
        getInstalledExtensionWithoutBuild();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresExtensions(extensions);
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions()
        .addAll(toInstallExtensions);
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions()
        .add(installedExtension);

    StackGresShardedDbOps result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    result.getSpec().getMajorVersionUpgrade().getPostgresExtensions()
        .forEach(anExtension -> assertNotNull(anExtension.getVersion()));
    assertEquals(toInstallExtensions, result.getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions());
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

  private static void disableRegistry(StackGresShardedCluster cluster) {
    if (cluster.getSpec().getConfigurations() == null) {
      cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    }
    cluster.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    cluster.getSpec().getConfigurations().getRegistry().setEnabled(false);
  }

}
