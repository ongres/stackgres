/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresDistributedLogsContext> {

  @Inject
  public DistributedLogsDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresDistributedLogsContext>> instance) {
    super(instance);
  }

}
