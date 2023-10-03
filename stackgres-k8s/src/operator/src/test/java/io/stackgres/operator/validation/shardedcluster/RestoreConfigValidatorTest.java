/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
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

  @Mock
  private CustomResourceFinder<StackGresShardedBackup> finder;

  private RestoreConfigValidator validator;

  private StackGresShardedBackup backup;

  @BeforeEach
  void setUp() {
    validator = new RestoreConfigValidator(finder);
    backup = Fixtures.shardedBackupList().loadDefault().get().getItems().get(0);
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(firstPgMajorVersion);

    StackGresShardedBackup backup =
        Fixtures.shardedBackupList().loadDefault().get().getItems().get(0);
    backup.getStatus().getBackupInformation()
        .setPostgresVersion(firstPgMajorVersion);
    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    validator.validate(review);

    verify(finder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAInvalidCreation_shouldFail() {
    final StackGresShardedClusterReview review = getCreationReview();

    StackGresShardedCluster cluster = review.getRequest().getObject();
    StackGresShardedClusterRestore restoreConfig = cluster.getSpec().getInitialData().getRestore();
    String backupName = restoreConfig.getFromBackup().getName();

    when(finder.findByNameAndNamespace(anyString(), anyString())).thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "SGShardedBackup " + backupName + " not found");

    verify(finder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenACreationWithBackupFromDifferentPgVersion_shouldFail() {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(secondPgMajorVersion);
    final String backupName = backup.getMetadata().getName();
    review.getRequest().getObject().getSpec().getInitialData().getRestore().getFromBackup()
        .setName(backupName);

    backup.getStatus().getBackupInformation()
        .setPostgresVersion(firstPgMajorVersion);

    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot restore from SGShardedBackup " + backupName
        + " because it comes from a different postgres major version");

    verify(finder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenACreationWithWrongNumberOfSgBackups_shouldFail() {
    final StackGresShardedClusterReview review = getCreationReview();
    backup.getStatus().setSgBackups(List.of("test"));
    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "sgBackups must be an array of size 3 (the coordinator plus the number of shards)"
        + " but was 1");
  }

  @Test
  void givenACreationWithNullSgBackups_shouldFail() {
    final StackGresShardedClusterReview review = getCreationReview();
    backup.getStatus().setSgBackups(null);
    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "sgBackups must be an array of size 3 (the coordinator plus the number of shards)"
        + " but was null");
  }

  @Test
  void givenACreationWithNullBackupStatus_shouldFail() {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getInitialData().getRestore()
        .getFromBackup().setName(backup.getMetadata().getName());
    backup.setStatus(null);
    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot restore from SGShardedBackup "
            + backup.getMetadata().getName() + " because it's not ready");
  }

  @Test
  void givenACreationWithNoRestoreConfig_shouldDoNothing() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdateWithSameRestoreConfig_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getInitialData().getRestore().getFromBackup()
        .setName(review.getRequest().getOldObject().getSpec().getInitialData().getRestore()
            .getFromBackup().getName());

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdateWithoutInitialData_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().setInitialData(null);
    review.getRequest().getOldObject().getSpec().setInitialData(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdateWithoutRestoreConfig_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);
    review.getRequest().getOldObject().getSpec().getInitialData().setRestore(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenAnUpdate_shouldFail() {
    final StackGresShardedClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update SGShardedCluster's restore configuration");

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  private StackGresShardedClusterReview getCreationReview() {
    return AdmissionReviewFixtures.shardedCluster().loadCreate().get();
  }

  private StackGresShardedClusterReview getUpdateReview() {
    return AdmissionReviewFixtures.shardedCluster().loadRestoreConfigUpdate().get();
  }

}
