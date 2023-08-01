/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.InitContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;

@ApplicationScoped
public class DistributedLogsInitContainerFactoryDiscover
    extends InitContainerFactoryDiscoverer<DistributedLogsContainerContext> {

  @Inject
  public DistributedLogsInitContainerFactoryDiscover(
      @Any Instance<ContainerFactory<DistributedLogsContainerContext>> instance) {
    super(instance);
  }

}
