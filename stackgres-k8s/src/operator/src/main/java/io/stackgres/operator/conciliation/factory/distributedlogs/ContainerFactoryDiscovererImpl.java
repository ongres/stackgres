/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ContainerFactoryDiscoverer;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;

@ApplicationScoped
public class ContainerFactoryDiscovererImpl
    extends ResourceDiscoverer<ContainerFactory<DistributedLogsContainerContext>>
    implements ContainerFactoryDiscoverer<DistributedLogsContainerContext> {

  @Inject
  public ContainerFactoryDiscovererImpl(
      @RunningContainer
          Instance<ContainerFactory<DistributedLogsContainerContext>> instance) {
    init(instance);
    resourceHub.forEach((key, value) -> {
      value.sort((f1, f2) -> {
        int f1Order = f1.getClass().getAnnotation(RunningContainer.class)
            .order();
        int f2Order = f2.getClass().getAnnotation(RunningContainer.class)
            .order();
        return Integer.compare(f1Order, f2Order);
      });
    });
  }

  @Override
  public List<ContainerFactory<DistributedLogsContainerContext>> discoverContainers(
      DistributedLogsContainerContext context) {
    final StackGresDistributedLogs cluster = context.getDistributedLogsContext().getSource();
    StackGresVersion version = StackGresVersion.getClusterStackGresVersion(cluster);
    return resourceHub.get(version).stream()
        .filter(f -> f.isActivated(context))
        .collect(Collectors.toUnmodifiableList());
  }
}
