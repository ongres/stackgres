/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreConfigValidatorTest {

  private static final String firstPgMajorVersion =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .get(0).get();

  private static final String secondPgMajorVersion =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .get(1).get();

  private static final String firstPgMajorVersionNumber =
      Seq.of(StackGresComponent.POSTGRESQL.getLatest().getVersion(firstPgMajorVersion)
          .split("\\.")).map(Integer::valueOf).append(1)
          .map(number -> String.format("%02d", number)).toString();

  @Mock
  private CustomResourceFinder<StackGresBackup> finder;

  private RestoreConfigValidator validator;

  private StackGresBackup backup;

  @BeforeEach
  void setUp() {
    validator = new RestoreConfigValidator(finder);
    backup = Fixtures.backupList().loadDefault().get().getItems().get(0);
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(firstPgMajorVersion);

    StackGresBackup backup =
        Fixtures.backupList().loadDefault().get().getItems().get(0);
    backup.getStatus().getBackupInformation()
        .setPostgresVersion(firstPgMajorVersionNumber);
    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    validator.validate(review);

    verify(finder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAInvalidCreation_shouldFail() {
    final StackGresClusterReview review = getCreationReview();

    StackGresCluster cluster = review.getRequest().getObject();
    StackGresClusterRestore restoreConfig = cluster.getSpec().getInitialData().getRestore();
    String backupName = restoreConfig.getFromBackup().getName();

    when(finder.findByNameAndNamespace(anyString(), anyString())).thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "SGBackup " + backupName + " not found");

    verify(finder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenACreationWithBackupFromDifferentPgVersion_shouldFail() {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(secondPgMajorVersion);
    final String backupName = backup.getMetadata().getName();
    review.getRequest().getObject().getSpec().getInitialData().getRestore().getFromBackup()
        .setName(backupName);

    backup.getStatus().getBackupInformation()
        .setPostgresVersion(firstPgMajorVersionNumber);

    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot restore from SGBackup " + backupName
            + " because it comes from an incompatible postgres version");

    verify(finder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenACreationWithBackupFromAndUid_shouldFail() {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getInitialData().getRestore()
        .getFromBackup().setName(null);
    review.getRequest().getObject().getSpec().getInitialData().getRestore()
        .getFromBackup().setUid("23442867-377d-11ea-b04b-0242ac110004");

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "uid is deprecated, use name instead!");

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenACreationWithNoRestoreConfig_shouldDoNothing() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdateWithSameRestoreConfig_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getInitialData().getRestore().getFromBackup()
        .setName(review.getRequest().getOldObject().getSpec().getInitialData().getRestore()
            .getFromBackup().getName());

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdateWithoutInitialData_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().setInitialData(null);
    review.getRequest().getOldObject().getSpec().setInitialData(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdateWithoutRestoreConfig_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);
    review.getRequest().getOldObject().getSpec().getInitialData().setRestore(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdate_shouldFail() {
    final StackGresClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update SGCluster's restore configuration");

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  private StackGresClusterReview getCreationReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  private StackGresClusterReview getUpdateReview() {
    return AdmissionReviewFixtures.cluster().loadRestoreConfigUpdate().get();
  }

}
