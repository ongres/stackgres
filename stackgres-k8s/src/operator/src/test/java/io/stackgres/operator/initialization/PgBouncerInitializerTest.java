/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.utils.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PgBouncerInitializerTest extends AbstractInitializerTest<StackGresPgbouncerConfig> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresPgbouncerConfig> getInstance(
      CustomResourceFinder<StackGresPgbouncerConfig> resourceFinder,
      CustomResourceScheduler<StackGresPgbouncerConfig> resourceScheduler,
      DefaultCustomResourceFactory<StackGresPgbouncerConfig> resourceFactory,
      InitializationQueue queue) {
    return new PgBouncerInitializer(resourceFinder, resourceScheduler, resourceFactory, queue);
  }

  @Override
  StackGresPgbouncerConfig getDefaultCR() {
    return JsonUtil
        .readFromJson("pgbouncer_config/default.json", StackGresPgbouncerConfig.class);
  }
}
