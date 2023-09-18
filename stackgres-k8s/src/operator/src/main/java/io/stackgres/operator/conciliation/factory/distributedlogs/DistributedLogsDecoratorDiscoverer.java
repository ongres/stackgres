/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;

@ApplicationScoped
public class DistributedLogsDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresDistributedLogsContext> {

  @Inject
  public DistributedLogsDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresDistributedLogsContext>> instance) {
    super(instance);
  }

}
