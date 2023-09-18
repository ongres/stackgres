/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;

@ApplicationScoped
public class ConfigDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresConfigContext> {

  @Inject
  public ConfigDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresConfigContext>> instance) {
    super(instance);
  }

}
