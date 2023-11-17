/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.operator.conciliation.InitContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterInitContainerFactoryDiscoverer
    extends InitContainerFactoryDiscoverer<ClusterContainerContext> {

  @Inject
  public ClusterInitContainerFactoryDiscoverer(
      @Any Instance<ContainerFactory<ClusterContainerContext>> instance) {
    super(instance);
  }

}
