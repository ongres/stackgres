/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultBackupConfigKeeperTest
    extends DefaultKeeperTest<StackGresBackupConfig, BackupConfigReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresBackupConfig, BackupConfigReview>
      getValidatorInstance() {
    return new DefaultBackupConfigKeeper();
  }

  @Override
  protected BackupConfigReview getCreationSample() {
    return JsonUtil.readFromJson("backupconfig_allow_request/create.json",
        BackupConfigReview.class);
  }

  @Override
  protected BackupConfigReview getDeleteSample() {
    return JsonUtil.readFromJson("backupconfig_allow_request/delete.json",
        BackupConfigReview.class);
  }

  @Override
  protected BackupConfigReview getUpdateSample() {
    return JsonUtil.readFromJson("backupconfig_allow_request/update.json",
        BackupConfigReview.class);
  }

}
