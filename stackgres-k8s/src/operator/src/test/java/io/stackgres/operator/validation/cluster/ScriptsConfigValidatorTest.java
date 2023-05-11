/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptsConfigValidatorTest {

  private ScriptsConfigValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ScriptsConfigValidator();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();

    validator.validate(review);
  }

  @Test
  void givenACreationWithDuplicatedId_shouldFail() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();

    review.getRequest().getObject().getSpec().getManagedSql().getScripts().get(0).setId(1);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must contain unique ids");
  }

  @Test
  void givenACreationWithUnrelatedStatusId_shouldFail() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();

    review.getRequest().getObject().getStatus().getManagedSql().getScripts().remove(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must contain a matching id"
            + " for each script status entry");
  }

  @Test
  void givenACreationWithUnrelatedId_shouldFail() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();

    review.getRequest().getObject().getSpec().getManagedSql().getScripts().remove(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script status entries must contain a matching id"
            + " for each script entry");
  }

  private StackGresClusterReview getCreationReview() {
    return AdmissionReviewFixtures.cluster()
        .loadCreateWithManagedSql().get();
  }

}
