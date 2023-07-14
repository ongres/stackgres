/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import io.stackgres.operator.conciliation.AnnotatedResourceDiscoverer;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ContainerFactoryDiscovererImpl
    extends AnnotatedResourceDiscoverer<ContainerFactory<ClusterContainerContext>,
        RunningContainer>
    implements ContainerFactoryDiscoverer<ClusterContainerContext> {

  @Inject
  public ContainerFactoryDiscovererImpl(
      @Any
      Instance<ContainerFactory<ClusterContainerContext>> instance) {
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
  public List<ContainerFactory<ClusterContainerContext>> discoverContainers(
      ClusterContainerContext context) {
    return resourceHub.get(context.getClusterContext().getVersion()).stream()
        .filter(f -> f.isActivated(context))
        .toList();
  }
}
