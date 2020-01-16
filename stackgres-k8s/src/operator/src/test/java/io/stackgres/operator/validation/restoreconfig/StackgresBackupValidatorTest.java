/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import java.util.Optional;
import java.util.UUID;

import io.stackgres.operator.WithRestoreReviewResources;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.stackgres.operator.utils.ValidationUtils.assertValidationFailed;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StackgresBackupValidatorTest implements WithRestoreReviewResources {

  @Mock
  private KubernetesCustomResourceScanner<StackGresBackup> backupScanner;

  private final StackGresBackupList backupList = JsonUtil
      .readFromJson("backup/list.json", StackGresBackupList.class);

  private StackgresBackupValidator validator;

  @BeforeEach
  void setUp() {
    validator = new StackgresBackupValidator(backupScanner);
  }

  @Test
  void validBackup_shouldNotFail() throws ValidationFailed {

    when(backupScanner.findResources()).thenReturn(Optional.of(backupList.getItems()));

    String stackgresBackupUid = backupList.getItems().get(0).getMetadata().getUid();

    RestoreConfigReview review = prepareReview(stackgresBackupUid);

    validator.validate(review);

    verify(backupScanner).findResources();

  }

  @Test
  void noBackupsInCluster_shouldFail() {

    when(backupScanner.findResources()).thenReturn(Optional.empty());

    String stackgresBackup = UUID.randomUUID().toString();
    RestoreConfigReview review = prepareReview(stackgresBackup);

    assertValidationFailed(() -> validator.validate(review),
        "Backup " + stackgresBackup + " doesn't exists");

    verify(backupScanner).findResources();
  }

  @Test
  void nonExistentBackup_shouldFail() {

    when(backupScanner.findResources()).thenReturn(Optional.of(backupList.getItems()));

    String stackgresBackup = UUID.randomUUID().toString();
    RestoreConfigReview review = prepareReview(stackgresBackup);

    assertValidationFailed(() -> validator.validate(review),
        "Backup " + stackgresBackup + " doesn't exists");

    verify(backupScanner).findResources();

  }

  private RestoreConfigReview prepareReview(String stackgresBackup){
    RestoreConfigReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getSource().setStorage(null);
    review.getRequest().getObject().getSpec().getSource().setBackupName(null);
    review.getRequest().getObject().getSpec().getSource().setStackgresBackup(stackgresBackup);
    return review;
  }
}