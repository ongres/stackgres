/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresStreamContext> {

  @Inject
  public StreamDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresStreamContext>> instance) {
    super(instance);
  }

}
