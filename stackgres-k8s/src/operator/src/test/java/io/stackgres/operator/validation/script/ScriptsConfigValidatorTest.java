/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.StackGresScriptReview;
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
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");

    validator.validate(review);
  }

  @Test
  void givenACreationWithDuplicatedId_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getSpec().getScripts().get(0).setId(1);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must contain unique ids");
  }

  @Test
  void givenACreationWithUnrelatedStatusId_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getStatus().getScripts().remove(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must contain a matching id"
            + " for each script status entry");
  }

  @Test
  void givenACreationWithUnrelatedId_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getSpec().getScripts().remove(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script status entries must contain a matching id"
            + " for each script entry");
  }

  private StackGresScriptReview getCreationReview() {
    return AdmissionReviewFixtures.script()
        .loadCreate().get();
  }

  private void prepareForScript(StackGresScriptReview review, String name, String script) {
    review.getRequest().getObject().getSpec().getScripts().forEach(s -> {
      s.setScriptFrom(null);
      s.setName(name);
      s.setScript(script);
    });
  }

}
