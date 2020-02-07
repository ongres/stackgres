/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PgBouncerInitializerTest extends AbstractInitializerTest<StackGresPgbouncerConfig> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresPgbouncerConfig> getInstance() {
    return new PgBouncerInitializer();
  }

  @Override
  StackGresPgbouncerConfig getDefaultCR() {
    return JsonUtil
        .readFromJson("pgbouncer_config/default.json", StackGresPgbouncerConfig.class);
  }
}
