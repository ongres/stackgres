/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class DecoratorDiscovererImpl
    extends ResourceDiscoverer<Decorator<StackGresDistributedLogs>>
    implements DecoratorDiscoverer<StackGresDistributedLogs> {

  @Inject
  public DecoratorDiscovererImpl(
      @Any Instance<Decorator<StackGresDistributedLogs>> instance) {
    init(instance);

  }

  @Override
  public List<Decorator<StackGresDistributedLogs>> discoverDecorator(
      StackGresDistributedLogs context) {

    StackGresVersion version = StackGresVersion.getClusterStackGresVersion(context);
    return resourceHub.get(version).stream()
        .collect(Collectors.toUnmodifiableList());

  }
}
