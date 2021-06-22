/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

public abstract class AbstractDefaultCustomResourceInitializer<T extends CustomResource<?, ?>>
    implements DefaultCustomResourceInitializer {

  @Inject
  CustomResourceScheduler<T> customResourceScheduler;

  @Inject
  DefaultFactoryProvider<DefaultCustomResourceFactory<T>> factoryProvider;

  @Inject
  CustomResourceScanner<T> resourceScanner;

  @Override
  public void initialize() {
    factoryProvider.getFactories().forEach(factory -> {
      CustomResourceInitializer<T> customResourceInitializer =
          new CustomResourceInitializer<>(customResourceScheduler, factory, resourceScanner);

      customResourceInitializer.initialize();

    });
  }

}
