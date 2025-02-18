/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.operator.common.StackGresBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterReferenceValidatorTest {

  private ClusterValidator validator;

  @BeforeEach
  void setUp() throws Exception {
    validator = new ClusterValidator();
  }

  @Test
  void giveAnAttemptToUpdateReferencedCluster_shouldFail() {
    final StackGresBackupReview review = AdmissionReviewFixtures.backup().loadUpdate().get();
    review.getRequest().getObject().getStatus().setSgBackupConfig(null);

    review.getRequest().getObject().getSpec().setSgCluster("test");

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGBackup sgCluster can not be updated.", resultMessage);
  }

  @Test
  void giveAnAttemptToUpdateManagedLifecycle_shouldNotFail() throws ValidationFailed {
    final StackGresBackupReview review = AdmissionReviewFixtures.backup().loadUpdate().get();
    review.getRequest().getObject().getStatus().setSgBackupConfig(null);

    review.getRequest().getObject().getSpec().setManagedLifecycle(
        !review.getRequest().getObject().getSpec().getManagedLifecycle());

    validator.validate(review);
  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {
    final StackGresBackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);
  }

}
