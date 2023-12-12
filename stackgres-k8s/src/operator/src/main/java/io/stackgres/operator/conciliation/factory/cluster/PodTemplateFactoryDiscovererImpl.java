/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.operator.conciliation.AbstractDiscoverer;
import io.stackgres.operator.conciliation.factory.PodTemplateFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class PodTemplateFactoryDiscovererImpl
    extends AbstractDiscoverer<PodTemplateFactory<ClusterContainerContext>>
    implements PodTemplateFactoryDiscoverer<ClusterContainerContext> {

  @Inject
  public PodTemplateFactoryDiscovererImpl(
      @Any Instance<PodTemplateFactory<ClusterContainerContext>> instance) {
    super(instance);
  }

  @Override
  public PodTemplateFactory<ClusterContainerContext> discoverPodSpecFactory(
      ClusterContainerContext context) {
    var podTemplateFactories = hub.get(context.getClusterContext().getVersion());

    if (podTemplateFactories.size() != 1) {
      throw new IllegalArgumentException(
          "It should be a single pod template factory per StackGres Version");
    }
    return podTemplateFactories.get(0);
  }
}
