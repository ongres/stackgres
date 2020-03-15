/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class BackupConfigDependenciesValidatorTest
    extends DependenciesValidatorTest<BackupConfigReview, BackupConfigDependenciesValidator> {

  @Override
  protected DependenciesValidator<BackupConfigReview> setUpValidation() {
    return new BackupConfigDependenciesValidator();
  }

  @Override
  @Test
  protected void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/create.json",
        BackupConfigReview.class);

    givenAReviewCreation_itShouldDoNothing(review);

  }

  @Override
  @Test
  protected void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/update.json",
        BackupConfigReview.class);

    givenAReviewUpdate_itShouldDoNothing(review);

  }

  @Override
  @Test
  protected void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);

    givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(review);

  }

  @Override
  @Test
  protected void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);

    givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(review);

  }
}
