/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigInitializerTest extends AbstractInitializerTest<StackGresBackupConfig> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresBackupConfig> getInstance(
      CustomResourceFinder<StackGresBackupConfig> resourceFinder,
      CustomResourceScheduler<StackGresBackupConfig> resourceScheduler,
      DefaultCustomResourceFactory<StackGresBackupConfig> resourceFactory,
      InitializationQueue queue) {
    return new BackupConfigInitializer(resourceFinder, resourceScheduler, resourceFactory, queue);
  }

  @Override
  StackGresBackupConfig getDefaultCR() {
    return JsonUtil
        .readFromJson("backup_config/default.json", StackGresBackupConfig.class);
  }
}