/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreConfigValidatorTest {

  private static final String firstPgMajorVersion =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .get(0).get();

  private RestoreConfigValidator validator;

  @BeforeEach
  void setUp() {
    validator = new RestoreConfigValidator();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(firstPgMajorVersion);

    StackGresShardedBackup backup =
        Fixtures.shardedBackupList().loadDefault().get().getItems().get(0);
    backup.getStatus().getBackupInformation()
        .setPostgresVersion(firstPgMajorVersion);
    validator.validate(review);
  }

  @Test
  void givenACreationWithNoRestoreConfig_shouldDoNothing() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);

    validator.validate(review);
  }

  @Test
  void givenAnUpdateWithSameRestoreConfig_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getInitialData().getRestore().getFromBackup()
        .setName(review.getRequest().getOldObject().getSpec().getInitialData().getRestore()
            .getFromBackup().getName());

    validator.validate(review);
  }

  @Test
  void givenAnUpdateWithoutInitialData_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().setInitialData(null);
    review.getRequest().getOldObject().getSpec().setInitialData(null);

    validator.validate(review);
  }

  @Test
  void givenAnUpdateWithoutRestoreConfig_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);
    review.getRequest().getOldObject().getSpec().getInitialData().setRestore(null);

    validator.validate(review);
  }

  @Test
  void givenAnUpdate_shouldFail() {
    final StackGresShardedClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update restore configuration");
  }

  private StackGresShardedClusterReview getCreationReview() {
    return AdmissionReviewFixtures.shardedCluster().loadCreate().get();
  }

  private StackGresShardedClusterReview getUpdateReview() {
    return AdmissionReviewFixtures.shardedCluster().loadRestoreConfigUpdate().get();
  }

}
