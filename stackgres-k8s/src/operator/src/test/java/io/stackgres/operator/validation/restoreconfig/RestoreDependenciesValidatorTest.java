/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operator.common.RestoreConfigReview;

import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreDependenciesValidatorTest
    extends DependenciesValidatorTest<RestoreConfigReview, RestoreDependenciesValidator> {

  @BeforeEach
  void setUp() {
    validator = new RestoreDependenciesValidator(clusterScanner);
  }

  @Override
  @Test
  protected void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {
    RestoreConfigReview review = JsonUtil.readFromJson(
        "restore_config_allow_request/create.json", RestoreConfigReview.class);
    givenAReviewCreation_itShouldDoNothing(review);

  }

  @Override
  @Test
  protected void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

    RestoreConfigReview review = JsonUtil.readFromJson(
        "restore_config_allow_request/update.json", RestoreConfigReview.class);

    givenAReviewCreation_itShouldDoNothing(review);

  }

  @Override
  @Test
  protected void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() {

    RestoreConfigReview review = JsonUtil.readFromJson(
        "restore_config_allow_request/delete.json", RestoreConfigReview.class);

    givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(review);

  }

  @Override
  @Test
  protected void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt()
      throws ValidationFailed {

    RestoreConfigReview review = JsonUtil.readFromJson(
        "restore_config_allow_request/delete.json", RestoreConfigReview.class);

    givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(review);

  }
}