/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresConfigInitializerTest extends AbstractInitializerTest<StackGresPostgresConfig> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresPostgresConfig> getInstance(
      CustomResourceFinder<StackGresPostgresConfig> resourceFinder,
      CustomResourceScheduler<StackGresPostgresConfig> resourceScheduler,
      DefaultCustomResourceFactory<StackGresPostgresConfig> resourceFactory) {
    return new PostgresConfigInitializer(resourceFinder, resourceScheduler, resourceFactory);
  }

  @Override
  StackGresPostgresConfig getDefaultCR() {
    return JsonUtil
        .readFromJson("postgres_config/default_postgres.json", StackGresPostgresConfig.class);
  }
}