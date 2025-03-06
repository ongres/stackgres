/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterReferenceValidatorTest {

  private ShardedClusterValidator validator;

  @BeforeEach
  void setUp() throws Exception {
    validator = new ShardedClusterValidator();
  }

  @Test
  void giveAnAttemptToUpdateReferencedCluster_shouldFail() {
    final StackGresShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();

    review.getRequest().getObject().getSpec().setSgShardedCluster("test");

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGShardedBackup sgShardedCluster can not be updated.", resultMessage);
  }

  @Test
  void giveAnAttemptToUpdateManagedLifecycle_shouldNotFail() throws ValidationFailed {
    final StackGresShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();

    review.getRequest().getObject().getSpec().setManagedLifecycle(
        !review.getRequest().getObject().getSpec().getManagedLifecycle());

    validator.validate(review);
  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {
    final StackGresShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);
  }

}
