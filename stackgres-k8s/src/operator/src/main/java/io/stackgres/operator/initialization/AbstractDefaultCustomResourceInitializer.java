/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

public abstract class AbstractDefaultCustomResourceInitializer<T extends CustomResource>
    implements DefaultCustomResourceInitializer<T> {

  private CustomResourceScheduler<T> customResourceScheduler;
  private DefaultFactoryProvider<DefaultCustomResourceFactory<T>> factoryProvider;
  private CustomResourceScanner<T> resourceScanner;

  @Override
  public void initialize() {
    factoryProvider.getFactories().forEach(factory -> {
      CustomResourceInitializer<T> customResourceInitializer =
          new CustomResourceInitializer<>(customResourceScheduler, factory, resourceScanner);

      customResourceInitializer.initialize();

    });
  }

  @Inject
  public void setResourceScheduler(CustomResourceScheduler<T> resourceScheduler) {
    this.customResourceScheduler = resourceScheduler;
  }

  @Inject
  public void setFactoryProvider(
      DefaultFactoryProvider<DefaultCustomResourceFactory<T>> factoryProvider) {
    this.factoryProvider = factoryProvider;
  }

  @Inject
  public void setResourceScanner(CustomResourceScanner<T> resourceScanner) {
    this.resourceScanner = resourceScanner;
  }
}
