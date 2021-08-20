/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.InitContainerFactoryDiscover;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;

@ApplicationScoped
public class InitContainerFactoryDiscoverImpl
    extends ResourceDiscoverer<ContainerFactory<StackGresClusterContainerContext>>
    implements InitContainerFactoryDiscover<StackGresClusterContainerContext> {

  @Inject
  public InitContainerFactoryDiscoverImpl(
      @InitContainer
          Instance<ContainerFactory<StackGresClusterContainerContext>> instance) {
    init(instance);
    resourceHub.forEach((key, value) -> {
      value.sort((f1, f2) -> {
        int f1Order = f1.getClass().getAnnotation(InitContainer.class)
            .order();
        int f2Order = f2.getClass().getAnnotation(InitContainer.class)
            .order();
        return Integer.compare(f1Order, f2Order);
      });
    });
  }

  @Override
  public List<ContainerFactory<StackGresClusterContainerContext>> discoverContainers(
      StackGresClusterContainerContext context) {
    return resourceHub.get(context.getClusterContext().getVersion()).stream()
        .filter(f -> f.isActivated(context))
        .collect(Collectors.toUnmodifiableList());
  }
}
