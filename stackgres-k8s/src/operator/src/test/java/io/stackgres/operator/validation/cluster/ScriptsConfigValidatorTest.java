/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
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
  void givenAnUpdate_shouldFail() {

    final StackGresClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update cluster's scripts configuration");

  }

  private StackGresClusterReview getCreationReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackGresClusterReview.class);
  }

  private StackGresClusterReview getUpdateReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/scripts_config_update.json",
            StackGresClusterReview.class);
  }

}