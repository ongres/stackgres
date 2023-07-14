/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.stream.Collectors;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DecoratorDiscovererImpl
    extends ResourceDiscoverer<Decorator<StackGresClusterContext>>
    implements DecoratorDiscoverer<StackGresClusterContext> {

  @Inject
  public DecoratorDiscovererImpl(
      @Any Instance<Decorator<StackGresClusterContext>> instance) {
    init(instance);
  }

  @Override
  public List<Decorator<StackGresClusterContext>> discoverDecorator(
      StackGresClusterContext context) {
    return resourceHub.get(context.getVersion()).stream()
        .collect(Collectors.toUnmodifiableList());

  }
}
