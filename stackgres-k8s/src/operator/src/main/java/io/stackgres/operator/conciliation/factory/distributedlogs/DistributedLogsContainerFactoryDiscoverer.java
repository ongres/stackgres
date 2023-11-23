/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import io.stackgres.operator.conciliation.RunningContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsContainerFactoryDiscoverer
    extends RunningContainerFactoryDiscoverer<DistributedLogsContainerContext> {

  @Inject
  public DistributedLogsContainerFactoryDiscoverer(
      @Any Instance<ContainerFactory<DistributedLogsContainerContext>> instance) {
    super(instance);
  }

}
