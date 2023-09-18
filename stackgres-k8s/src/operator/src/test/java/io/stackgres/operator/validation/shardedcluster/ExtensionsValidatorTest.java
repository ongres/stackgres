/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.StackGresShardedClusterReview;
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
class ExtensionsValidatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions().findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedBuildVersions().findFirst().get();

  private ExtensionsValidator validator;

  private List<StackGresClusterInstalledExtension> installedExtensions;

  @Mock
  private ExtensionMetadataManager extensionMetadataManager;

  @Mock
  private CustomResourceScanner<StackGresCluster> clusterScanner;

  @Mock
  private LabelFactoryForShardedCluster labelFactory;

  @BeforeEach
  void setUp() {
    validator = new ExtensionsValidator(extensionMetadataManager,
        clusterScanner, labelFactory);

    installedExtensions = Seq.of(
            "citus",
            "citus_columnar")
        .map(this::getInstalledExtension)
        .collect(Collectors.toUnmodifiableList());
  }

  private List<StackGresExtensionMetadata> getDefaultExtensionsMetadata(
      InvocationOnMock invocation) {
    return installedExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((StackGresClusterExtension) invocation.getArgument(1))
                .getName()))
        .map(StackGresExtensionMetadata::new)
        .collect(Collectors.toUnmodifiableList());
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().setStatus(new StackGresShardedClusterStatus());
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(installedExtensions);
    validator.validate(review);
  }

  @Test
  void givenAnUpdate_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getUpdateReview();
    review.getRequest().getObject().setStatus(new StackGresShardedClusterStatus());
    review.getRequest().getObject().getStatus().setToInstallPostgresExtensions(new ArrayList<>());
    review.getRequest().getObject().getStatus().getToInstallPostgresExtensions()
        .addAll(installedExtensions);
    validator.validate(review);
  }

  @Test
  void givenACreationWithMissingExtensions_shouldFail() {
    final StackGresShardedClusterReview review = getCreationReview();
    when(extensionMetadataManager.getExtensionsAnyVersion(
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean())
    ).then(this::getDefaultExtensionsMetadata);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.EXTENSION_NOT_FOUND,
        "Some extensions were not found: citus 11.3-1 (available 1.0.0),"
            + " citus_columnar 11.3-1 (available 1.0.0)");
  }

  private StackGresShardedClusterReview getCreationReview() {
    StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    return review;
  }

  private StackGresShardedClusterReview getUpdateReview() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadPostgresConfigUpdate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
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

}
