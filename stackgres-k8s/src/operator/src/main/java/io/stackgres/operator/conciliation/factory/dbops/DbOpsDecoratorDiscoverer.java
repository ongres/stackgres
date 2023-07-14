/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresDbOpsContext> {

  @Inject
  public DbOpsDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresDbOpsContext>> instance) {
    super(instance);
  }

}
