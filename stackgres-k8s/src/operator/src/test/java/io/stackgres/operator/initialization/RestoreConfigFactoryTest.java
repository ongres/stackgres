/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreConfigFactoryTest extends AbstractInitializerTest<StackgresRestoreConfig> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackgresRestoreConfig> getInstance() {
    return new DefultRestoreInitializer();
  }

  @Override
  StackgresRestoreConfig getDefaultCR() {
    return JsonUtil
        .readFromJson("restore_config/default.json",
            StackgresRestoreConfig.class);
  }
}