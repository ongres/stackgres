/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsMajorVersionUpgradeExtensionsValidatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedMajorVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedBuildVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private ShardedDbOpsMajorVersionUpgradeExtensionsValidator validator;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> installedExtensions;

  @Mock
  private ExtensionMetadataManager extensionMetadataManager;

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    validator = new ShardedDbOpsMajorVersionUpgradeExtensionsValidator(
        StackGresContextMock.CONTEXT,
        extensionMetadataManager,
        clusterFinder);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(
        StackGresKeys.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    disableRegistry(cluster);

    extensions = Seq.of(
            "plpgsql",
            "pg_stat_statements",
            "dblink",
            "plpython3u")
        .map(this::getExtension)
        .collect(Collectors.toUnmodifiableList());
    installedExtensions = Seq.of(
            "plpgsql",
            "pg_stat_statements",
            "dblink",
            "plpython3u")
        .map(this::getInstalledExtension)
        .collect(Collectors.toUnmodifiableList());
    when(clusterFinder.findByNameAndNamespace(
        any(), any()))
        .thenReturn(Optional.of(cluster));
  }

  private List<StackGresExtensionMetadata> getDefaultExtensionsMetadata(
      InvocationOnMock invocation) {
    return installedExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((StackGresClusterExtension) invocation.getArgument(1))
                .getName()))
        .map(StackGresExtensionMetadata::new)
        .toList();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresExtensions(extensions);
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getToInstallPostgresExtensions()
        .addAll(installedExtensions);
    validator.validate(review);
  }

  @Test
  void givenACreationWithMissingExtensions_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresExtensions(extensions);
    when(extensionMetadataManager.getExtensionsAnyVersion(
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean())
    ).then(this::getDefaultExtensionsMetadata);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.EXTENSION_NOT_FOUND,
        "Some extensions were not found: dblink 1.0.0 (available 1.0.0),"
            + " pg_stat_statements 1.0.0 (available 1.0.0), plpgsql 1.0.0 (available 1.0.0),"
            + " plpython3u 1.0.0 (available 1.0.0)");
  }

  private StackGresShardedDbOpsReview getCreationReview() {
    StackGresShardedDbOpsReview review =
        AdmissionReviewFixtures.shardedDbOps().loadMajorVersionUpgradeCreate().get();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresVersion(POSTGRES_VERSION);
    return review;
  }

  private StackGresClusterInstalledExtension getInstalledExtension(String name) {
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

  private StackGresClusterExtension getExtension(String name) {
    final StackGresClusterExtension extension =
        new StackGresClusterExtension();
    extension.setName(name);
    extension.setVersion("1.0.0");
    return extension;
  }

  private static void disableRegistry(StackGresShardedCluster cluster) {
    if (cluster.getSpec().getConfigurations() == null) {
      cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    }
    cluster.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    cluster.getSpec().getConfigurations().getRegistry().setEnabled(false);
  }

}
