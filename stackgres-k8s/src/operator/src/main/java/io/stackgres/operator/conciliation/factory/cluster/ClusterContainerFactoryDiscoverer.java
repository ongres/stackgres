/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.RunningContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;

@ApplicationScoped
public class ClusterContainerFactoryDiscoverer
    extends RunningContainerFactoryDiscoverer<ClusterContainerContext> {

  @Inject
  public ClusterContainerFactoryDiscoverer(
      @Any Instance<ContainerFactory<ClusterContainerContext>> instance) {
    super(instance);
  }

}
