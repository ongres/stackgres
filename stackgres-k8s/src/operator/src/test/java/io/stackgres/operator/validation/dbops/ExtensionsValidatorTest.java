/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.extension.ExtensionRequest;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.Operation;
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

  @Mock
  ClusterExtensionMetadataManager extensionMetadataManager;
  @Mock
  CustomResourceFinder<StackGresCluster> clusterFinder;
  StackGresCluster cluster;
  private DbOpsMajorVersionUpgradeExtensionValidator validator;
  private List<StackGresClusterExtension> extensions;
  private List<StackGresClusterInstalledExtension> installedExtensions;

  @BeforeEach
  void setUp() {
    validator = new DbOpsMajorVersionUpgradeExtensionValidator(extensionMetadataManager,
        clusterFinder);

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

    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setExtensions(extensions);
  }

  private List<StackGresExtensionMetadata> getDefaultExtensionsMetadata(
      InvocationOnMock invocation) {
    return installedExtensions.stream()
        .filter(defaultExtension -> defaultExtension.getName()
            .equals(((ExtensionRequest) invocation.getArgument(0))
                .getExtension().getName()))
        .map(StackGresExtensionMetadata::new)
        .collect(Collectors.toUnmodifiableList());
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final DbOpsReview review = getCreationReview();

    when(clusterFinder.findByNameAndNamespace(
        cluster.getMetadata().getName(),
        cluster.getMetadata().getNamespace()
    )).thenReturn(Optional.of(cluster));

    cluster.getSpec().setToInstallPostgresExtensions(List.copyOf(installedExtensions));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(any(), any());

  }

  @Test
  void givenAnUpdate_shouldPass() throws ValidationFailed {
    final DbOpsReview review = getUpdateReview();

    validator.validate(review);
    verify(clusterFinder, never()).findByNameAndNamespace(
        cluster.getMetadata().getName(),
        cluster.getMetadata().getNamespace()
    );
  }

  @Test
  void givenACreationWithMissingExtensions_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        cluster.getMetadata().getName(),
        cluster.getMetadata().getNamespace()
    )).thenReturn(Optional.of(cluster));
    final DbOpsReview review = getCreationReview();
    cluster.getSpec().getPostgres().setExtensions(extensions);

    when(extensionMetadataManager.requestExtensionsAnyVersion(
        any(ExtensionRequest.class),
        anyBoolean())
    ).then(this::getDefaultExtensionsMetadata);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.EXTENSION_NOT_FOUND,
        "Some extensions were not found: dblink (available 1.0.0),"
            + " pg_stat_statements (available 1.0.0), plpgsql (available 1.0.0),"
            + " plpython3u (available 1.0.0)");
  }

  private DbOpsReview getCreationReview() {
    DbOpsReview review = AdmissionReviewFixtures.dbOps().loadMajorVersionUpgradeCreate().get();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getMetadata().setNamespace(
        cluster.getMetadata().getNamespace()
    );
    review.getRequest().getObject().getSpec().setSgCluster(
        cluster.getMetadata().getName()
    );
    return review;
  }

  private DbOpsReview getUpdateReview() {
    DbOpsReview review = AdmissionReviewFixtures.dbOps().loadMajorVersionUpgradeCreate().get();
    review.getRequest().setOperation(Operation.UPDATE);
    review.getRequest().setOldObject(review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .setPostgresVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getMetadata().setNamespace(
        cluster.getMetadata().getNamespace()
    );
    review.getRequest().getObject().getSpec().setSgCluster(
        cluster.getMetadata().getName()
    );
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
