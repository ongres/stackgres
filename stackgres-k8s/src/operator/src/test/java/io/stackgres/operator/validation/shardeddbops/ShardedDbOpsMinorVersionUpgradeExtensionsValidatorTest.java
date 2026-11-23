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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
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
class ShardedDbOpsMinorVersionUpgradeExtensionsValidatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions().findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedBuildVersions().findFirst().get();

  private ShardedDbOpsMinorVersionUpgradeExtensionsValidator validator;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> installedExtensions;

  @Mock
  private ExtensionMetadataManager extensionMetadataManager;

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    validator = new ShardedDbOpsMinorVersionUpgradeExtensionsValidator(
        extensionMetadataManager,
        clusterFinder);
    cluster = Fixtures.shardedCluster().loadDefault().get();

    extensions = Seq.of(
            "plpgsql",
            "pg_stat_statements",
            "dblink",
            "plpython3u",
            "citus",
            "citus_columnar",
            "pg_cron")
        .map(this::getExtension)
        .collect(Collectors.toUnmodifiableList());
    installedExtensions = Seq.of(
            "plpgsql",
            "pg_stat_statements",
            "dblink",
            "plpython3u",
            "citus",
            "citus_columnar",
            "pg_cron")
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
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .setPostgresExtensions(extensions);
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .getToInstallPostgresExtensions()
        .addAll(installedExtensions);
    validator.validate(review);
  }

  @Test
  void givenACreationWithMissingExtensions_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .setPostgresExtensions(extensions);
    when(extensionMetadataManager.getExtensionsAnyVersion(
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean())
    ).then(this::getDefaultExtensionsMetadata);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.EXTENSION_NOT_FOUND,
        "Some extensions were not found: citus 1.0.0 (available 1.0.0),"
            + " citus_columnar 1.0.0 (available 1.0.0), dblink 1.0.0 (available 1.0.0),"
            + " pg_cron 1.0.0 (available 1.0.0),"
            + " pg_stat_statements 1.0.0 (available 1.0.0), plpgsql 1.0.0 (available 1.0.0),"
            + " plpython3u 1.0.0 (available 1.0.0)");
  }

  @Test
  void givenACreationForAClusterWithAnInstalledExtensionWithoutBuildForTheTargetVersion_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    final StackGresClusterInstalledExtension installedCitus = getInstalledExtension("citus");
    installedCitus.setVersion("0.1.0");
    cluster.getStatus().setExtensions(List.of(installedCitus));
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .setPostgresExtensions(extensions.stream()
            .filter(extension -> !extension.getName().equals("citus"))
            .toList());
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .setToInstallPostgresExtensions(installedExtensions.stream()
            .filter(extension -> !extension.getName().equals("citus"))
            .toList());
    when(extensionMetadataManager.getExtensionsAnyVersion(
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean())
    ).then(this::getDefaultExtensionsMetadata);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.EXTENSION_NOT_FOUND,
        "Extension was not found: citus 0.1.0 (available 1.0.0)");
  }

  @Test
  void givenACreationThatReplacesAnInstalledExtensionWithoutBuildForTheTargetVersion_shouldPass()
      throws ValidationFailed {
    final StackGresShardedDbOpsReview review = getCreationReview();
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    final StackGresClusterInstalledExtension installedCitus = getInstalledExtension("citus");
    installedCitus.setVersion("0.1.0");
    cluster.getStatus().setExtensions(List.of(installedCitus));
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .setPostgresExtensions(extensions);
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
        .setToInstallPostgresExtensions(installedExtensions);
    validator.validate(review);
  }

  private StackGresShardedDbOpsReview getCreationReview() {
    StackGresShardedDbOpsReview review =
        AdmissionReviewFixtures.shardedDbOps().loadMinorVersionUpgradeCreate().get();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade()
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

}
