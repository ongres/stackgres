/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.validation.DefaultCustomResourceHolder;

public abstract class AbstractDefaultCustomResourceInitializer<T extends CustomResource<?, ?>>
    implements DefaultCustomResourceInitializer {

  @Inject
  CustomResourceScheduler<T> customResourceScheduler;

  @Inject
  DefaultFactoryProvider<DefaultCustomResourceFactory<T>> factoryProvider;

  @Inject
  CustomResourceScanner<T> resourceScanner;

  @Inject
  DefaultCustomResourceHolder<T> holder;

  List<CustomResourceInitializer<T>> initializers;

  void onStart(@Observes StartupEvent ev) {
    initializers = factoryProvider.getFactories().stream()
        .map(factory -> new CustomResourceInitializer<>(
            customResourceScheduler,
            factory,
            resourceScanner,
            holder
        ))
        .peek(CustomResourceInitializer::loadGeneratedResources)
        .toList();
  }

  @Override
  public void initialize() {
    initializers.forEach(CustomResourceInitializer::initialize);
  }

}
