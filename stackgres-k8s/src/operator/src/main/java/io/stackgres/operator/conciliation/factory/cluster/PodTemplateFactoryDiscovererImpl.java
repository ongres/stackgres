/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.factory.PodTemplateFactory;

@ApplicationScoped
public class PodTemplateFactoryDiscovererImpl
    extends ResourceDiscoverer<PodTemplateFactory<ClusterContainerContext>>
    implements PodTemplateFactoryDiscoverer<ClusterContainerContext> {

  @Inject
  public PodTemplateFactoryDiscovererImpl(
      @Any
          Instance<PodTemplateFactory<ClusterContainerContext>> instance) {
    init(instance);
  }

  @Override
  public PodTemplateFactory<ClusterContainerContext> discoverPodSpecFactory(
      ClusterContainerContext context) {
    var podTemplateFactories = resourceHub.get(context.getClusterContext().getVersion());

    if (podTemplateFactories.size() != 1) {
      throw new IllegalStateException(
          "It should be a single pod template factory per StackGres Version");
    }
    return podTemplateFactories.get(0);
  }
}
