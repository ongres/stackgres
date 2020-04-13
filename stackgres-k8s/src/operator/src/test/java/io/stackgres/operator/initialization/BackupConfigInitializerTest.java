/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigInitializerTest extends AbstractInitializerTest<StackGresBackupConfig> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresBackupConfig> getInstance() {
    return new BackupConfigInitializer();
  }

  @Override
  StackGresBackupConfig getDefaultCR() {
    return JsonUtil
        .readFromJson("backup_config/default.json", StackGresBackupConfig.class);
  }
}