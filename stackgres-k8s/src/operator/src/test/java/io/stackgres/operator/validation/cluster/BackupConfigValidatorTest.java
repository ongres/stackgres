/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class BackupConfigValidatorTest {

  private BackupConfigValidator validator;

  @BeforeEach
  void setUp() {
    validator = new BackupConfigValidator();
  }

  @Test
  void giveStackGresBackupOnCreation_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("spec.configuration.sgBackupConfig and spec.configuration.backupPath "
        + "are deprecated, please use the new spec.configuration.backups section "
        + "and define a SGObjectStorage", resultMessage);
  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownBackupConfig_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadBackupConfigUpdate().get();

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("spec.configuration.sgBackupConfig and spec.configuration.backupPath "
        + "are deprecated, please use the new spec.configuration.backups section "
        + "and define a SGObjectStorage", resultMessage);
  }

  @Test
  void giveAnAttemptToDeleteCluster_shouldNotFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadBackupConfigUpdate().get();

    review.getRequest().setOperation(Operation.DELETE);
    assertDoesNotThrow(() -> validator.validate(review));
  }

}
