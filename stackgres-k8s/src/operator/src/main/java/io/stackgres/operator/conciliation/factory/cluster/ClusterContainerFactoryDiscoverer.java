/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.operator.conciliation.RunningContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterContainerFactoryDiscoverer
    extends RunningContainerFactoryDiscoverer<ClusterContainerContext> {

  @Inject
  public ClusterContainerFactoryDiscoverer(
      @Any Instance<ContainerFactory<ClusterContainerContext>> instance) {
    super(instance);
  }

}
