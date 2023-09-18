/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.RunningContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;

@ApplicationScoped
public class DistributedLogsContainerFactoryDiscoverer
    extends RunningContainerFactoryDiscoverer<DistributedLogsContainerContext> {

  @Inject
  public DistributedLogsContainerFactoryDiscoverer(
      @Any Instance<ContainerFactory<DistributedLogsContainerContext>> instance) {
    super(instance);
  }

}
