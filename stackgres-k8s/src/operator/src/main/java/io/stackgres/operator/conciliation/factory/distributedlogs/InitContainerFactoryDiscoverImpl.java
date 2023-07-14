/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;

import io.stackgres.operator.conciliation.AnnotatedResourceDiscoverer;
import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class InitContainerFactoryDiscoverImpl
    extends AnnotatedResourceDiscoverer<ContainerFactory<DistributedLogsContainerContext>,
        InitContainer>
    implements InitContainerFactoryDiscover<DistributedLogsContainerContext> {

  @Inject
  public InitContainerFactoryDiscoverImpl(
      @Any
      Instance<ContainerFactory<DistributedLogsContainerContext>> instance) {
    init(instance);
    resourceHub.forEach((key, value) -> {
      value.sort((f1, f2) -> {
        int f1Order = getAnnotation(f1, InitContainer.class)
            .value().ordinal();
        int f2Order = getAnnotation(f2, InitContainer.class)
            .value().ordinal();
        return Integer.compare(f1Order, f2Order);
      });
    });
  }

  @Override
  protected Class<InitContainer> getAnnotationClass() {
    return InitContainer.class;
  }

  @Override
  public List<ContainerFactory<DistributedLogsContainerContext>> discoverContainers(
      DistributedLogsContainerContext context) {
    return resourceHub.get(context.getDistributedLogsContext().getVersion()).stream()
        .filter(f -> f.isActivated(context))
        .toList();
  }
}
