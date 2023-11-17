/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import io.stackgres.operator.conciliation.InitContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsInitContainerFactoryDiscover
    extends InitContainerFactoryDiscoverer<DistributedLogsContainerContext> {

  @Inject
  public DistributedLogsInitContainerFactoryDiscover(
      @Any Instance<ContainerFactory<DistributedLogsContainerContext>> instance) {
    super(instance);
  }

}
