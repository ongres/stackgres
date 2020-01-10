/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.BackupConfigReview;
import io.stackgres.operatorframework.ValidationFailed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class BackupConfigDependentBackupsValidatorTest {

  protected BackupConfigDependentBackupsValidator validator;

  @Mock
  protected KubernetesCustomResourceScanner<StackGresBackup> backupScanner;


  @BeforeEach
  void setUp() {
      validator = new BackupConfigDependentBackupsValidator(backupScanner);
  }

  @Test
  protected void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/create.json",
        BackupConfigReview.class);

      givenAReviewCreation_itShouldDoNothing(review);

  }

  protected void givenAReviewCreation_itShouldDoNothing(BackupConfigReview review) throws ValidationFailed {

    validator.validate(review);

    verify(backupScanner, never()).findResources();
    verify(backupScanner, never()).findResources(anyString());

  }

  @Test
  protected void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/update.json",
        BackupConfigReview.class);

      givenAReviewUpdate_itShouldDoNothing(review);

  }

  protected void givenAReviewUpdate_itShouldDoNothing(BackupConfigReview review) throws ValidationFailed {
    validator.validate(review);

    verify(backupScanner, never()).findResources();
    verify(backupScanner, never()).findResources(anyString());
  }

  @Test
  protected void givenAReviewDelete_itShouldFailIfIsABackupDependsOnIt() {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);

      givenAReviewDelete_itShouldFailIfIsABackupDependsOnIt(review);

  }

  protected void givenAReviewDelete_itShouldFailIfIsABackupDependsOnIt(BackupConfigReview review) {

    StackGresBackupList backupList = JsonUtil.readFromJson("stackgres_backup/list.json",
            StackGresBackupList.class);

    when(backupScanner.findResources(review.getRequest().getNamespace()))
            .thenReturn(Optional.of(backupList.getItems()));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    assertEquals("Can't delete "
                    + review.getRequest().getResource().getResource()
                    + "." + review.getRequest().getKind().getGroup()
                    + " " + review.getRequest().getName() + " because the "
                    + StackGresBackupDefinition.NAME + " "
                    + backupList.getItems().get(0).getMetadata().getName() + " depends on it"
            , ex.getResult().getMessage());

  }

  @Test
  protected void givenAReviewDelete_itShouldNotFailIfNotBackupDependsOnIt() throws ValidationFailed {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);

      givenAReviewDelete_itShouldNotFailIfNotBackupDependsOnIt(review);

  }

  protected void givenAReviewDelete_itShouldNotFailIfNotBackupDependsOnIt(BackupConfigReview review) throws ValidationFailed {

    when(backupScanner.findResources(review.getRequest().getNamespace()))
            .thenReturn(Optional.empty());

    validator.validate(review);

    verify(backupScanner, never()).findResources();
    verify(backupScanner).findResources(review.getRequest().getNamespace());

  }
}
