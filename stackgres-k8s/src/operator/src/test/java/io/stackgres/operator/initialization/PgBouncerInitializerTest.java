/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfig;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PgBouncerInitializerTest extends AbstractInitializerTest<StackGresPoolingConfig> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresPoolingConfig> getInstance() {
    return new PgBouncerInitializer();
  }

  @Override
  StackGresPoolingConfig getDefaultCR() {
    return JsonUtil
        .readFromJson("pooling_config/default.json", StackGresPoolingConfig.class);
  }
}
