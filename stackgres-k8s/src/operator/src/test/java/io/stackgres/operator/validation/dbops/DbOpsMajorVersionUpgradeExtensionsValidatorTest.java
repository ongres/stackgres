/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.docir.DocirExtensionMetadata;
import io.stackgres.common.docir.DocirMetadataManager;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
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
class DbOpsMajorVersionUpgradeExtensionsValidatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedMajorVersions(StackGresContextMock.CONTEXT).findFirst().get();

  private static final String BUILD_REVISION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedTagVersions(StackGresContextMock.CONTEXT)
      .findFirst().get().getRevision().toString();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedTagVersions(StackGresContextMock.CONTEXT).findFirst().get().getBuild();

  private DbOpsMajorVersionUpgradeExtensionsValidator validator;

  private List<StackGresClusterExtension> extensions;

  private List<StackGresClusterInstalledExtension> installedExtensions;

  @Mock
  private StackGresContext context;

  @Mock
  private DocirMetadataManager docirMetadataManager;

  @Mock
  private ExtensionMetadataManager extensionMetadataManager;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    validator = new DbOpsMajorVersionUpgradeExtensionsValidator(
        context,
        extensionMetadataManager,
        clusterFinder);
    cluster = Fixtures.cluster().loadDefault().get();

    extensions = Seq.of(
        "auto_explain",
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getExtension)
        .collect(Collectors.toUnmodifiableList());
    installedExtensions = Seq.of(
        "auto_explain",
        "plpgsql",
        "pg_stat_statements",
        "dblink",
        "plpython3u")
        .map(this::getInstalledExtension)
        .collect(Collectors.toUnmodifiableList());
    when(clusterFinder.findByNameAndNamespace(
        any(), any()))
        .thenReturn(Optional.of(cluster));
    lenient().when(context.getMetadataManager())
        .thenReturn(docirMetadataManager);
    lenient().when(docirMetadataManager.getFlavors())
        .thenReturn(StackGresContextMock.CONTEXT.getMetadataManager().getFlavors());
    lenient().when(docirMetadataManager.getFlavors(nullable(URI.class)))
        .thenReturn(StackGresContextMock.CONTEXT.getMetadataManager().getFlavors());
  }

  private List<DocirExtensionMetadata> getDefaultExtensionsMetadata(
      InvocationOnMock invocation) {
    return installedExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((StackGresClusterExtension) invocation.getArgument(2))
                .getName()))
        .map(DocirExtensionMetadata::new)
        .toList();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresDbOpsReview review = getCreationReview();
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
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresExtensions(extensions);
    when(docirMetadataManager.getExtensionsAnyVersion(
        any(),
        any(StackGresCluster.class),
        any(StackGresClusterExtension.class),
        anyBoolean())
    ).then(this::getDefaultExtensionsMetadata);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.EXTENSION_NOT_FOUND,
        "Some extensions were not found: auto_explain (available 1.0.0),"
        + " dblink (available 1.0.0),"
        + " pg_stat_statements (available 1.0.0),"
        + " plpgsql 1.0.0 (available 1.0.0),"
        + " plpython3u 1.0.0 (available 1.0.0)");
  }

  private StackGresDbOpsReview getCreationReview() {
    StackGresDbOpsReview review = AdmissionReviewFixtures.dbOps().loadMajorVersionUpgradeCreate().get();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresVersion(POSTGRES_VERSION);
    return review;
  }

  private StackGresClusterInstalledExtension getInstalledExtension(String name) {
    final StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName(name);
    installedExtension.setRepository(OperatorProperty.EXTENSIONS_REPOSITORY_URLS.getString());
    installedExtension.setVersion("1.0.0");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    installedExtension.setBuild(BUILD_REVISION);
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
