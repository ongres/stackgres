/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
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
  protected BackupConfigReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return JsonUtil.readFromJson("backupconfig_allow_request/create.json",
        BackupConfigReview.class);
  }

  @Override
  protected BackupConfigReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return JsonUtil.readFromJson("backupconfig_allow_request/update.json",
        BackupConfigReview.class);
  }

  @Override
  protected BackupConfigReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);
  }

  @Override
  protected BackupConfigReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
      throws ValidationFailed {
    return JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);
  }

  @Override
  protected BackupConfigReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().getConfiguration().setBackupConfig(null);
  }
}
