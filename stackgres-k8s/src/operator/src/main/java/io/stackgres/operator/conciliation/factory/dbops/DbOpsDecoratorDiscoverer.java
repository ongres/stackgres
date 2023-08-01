/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;

@ApplicationScoped
public class DbOpsDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresDbOpsContext> {

  @Inject
  public DbOpsDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresDbOpsContext>> instance) {
    super(instance);
  }

}
