/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class BackupConfigValidatorTest {

  private BackupConfigValidator validator;

  @Mock()
  private AbstractCustomResourceFinder<StackGresBackupConfig> configFinder;

  private StackGresBackupConfig backupConfig;

  @BeforeEach
  void setUp(){
    validator = new BackupConfigValidator(configFinder, new ConfigLoader());

    backupConfig = JsonUtil.readFromJson("backup_config/default.json", StackGresBackupConfig.class);

  }

  @Test
  void givenValidStackGresBackupOnCreation_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    String backupConfig = review.getRequest().getObject().getSpec().getConfiguration().getBackupConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(backupConfig, namespace))
        .thenReturn(Optional.of(this.backupConfig));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(backupConfig), eq(namespace));

  }

  @Test
  void giveInvalidStackGresBackupOnCreation_shouldFail() {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    String backupConfig = review.getRequest().getObject().getSpec().getConfiguration().getBackupConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(backupConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Backup config " + backupConfig + " not found", resultMessage);

  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownBackupConfig_shouldFail() {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/backup_config_update.json", StackGresClusterReview.class);

    String backupConfig = review.getRequest().getObject().getSpec().getConfiguration().getBackupConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(backupConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update to backup config " + backupConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(backupConfig), eq(namespace));

  }

  @Test
  void giveAnAttemptToUpdateToAnKnownBackup_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/backup_config_update.json", StackGresClusterReview.class);

    String backupConfig = review.getRequest().getObject().getSpec().getConfiguration().getBackupConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(backupConfig, namespace))
        .thenReturn(Optional.of(this.backupConfig));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(backupConfig), eq(namespace));

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/backup_config_update.json", StackGresClusterReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    String backupConfig = review.getRequest().getObject().getSpec().getConfiguration().getBackupConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(eq(backupConfig), eq(namespace));

  }

}
