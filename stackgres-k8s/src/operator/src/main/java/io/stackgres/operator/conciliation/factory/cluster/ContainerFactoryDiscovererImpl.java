/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.AnnotatedResourceDiscoverer;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;

@ApplicationScoped
public class ContainerFactoryDiscovererImpl
    extends AnnotatedResourceDiscoverer<ContainerFactory<StackGresClusterContainerContext>,
        RunningContainer>
    implements ContainerFactoryDiscoverer<StackGresClusterContainerContext> {

  @Inject
  public ContainerFactoryDiscovererImpl(
      @Any
      Instance<ContainerFactory<StackGresClusterContainerContext>> instance) {
    init(instance);
    resourceHub.forEach((key, value) -> {
      value.sort((f1, f2) -> {
        int f1Order = getAnnotation(f1, RunningContainer.class)
            .value().ordinal();
        int f2Order = getAnnotation(f2, RunningContainer.class)
            .value().ordinal();
        return Integer.compare(f1Order, f2Order);
      });
    });
  }

  @Override
  protected Class<RunningContainer> getAnnotationClass() {
    return RunningContainer.class;
  }

  @Override
  public List<ContainerFactory<StackGresClusterContainerContext>> discoverContainers(
      StackGresClusterContainerContext context) {
    return resourceHub.get(context.getClusterContext().getVersion()).stream()
        .filter(f -> f.isActivated(context))
        .toList();
  }
}
