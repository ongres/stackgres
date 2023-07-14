/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.stream.Collectors;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DecoratorDiscovererImpl
    extends ResourceDiscoverer<Decorator<StackGresDistributedLogsContext>>
    implements DecoratorDiscoverer<StackGresDistributedLogsContext> {

  @Inject
  public DecoratorDiscovererImpl(
      @Any Instance<Decorator<StackGresDistributedLogsContext>> instance) {
    init(instance);
  }

  @Override
  public List<Decorator<StackGresDistributedLogsContext>> discoverDecorator(
      StackGresDistributedLogsContext context) {
    return resourceHub.get(context.getVersion()).stream()
        .collect(Collectors.toUnmodifiableList());
  }

}
