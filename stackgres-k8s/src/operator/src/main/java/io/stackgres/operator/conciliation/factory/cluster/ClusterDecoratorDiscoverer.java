/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;

@ApplicationScoped
public class ClusterDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresClusterContext> {

  @Inject
  public ClusterDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresClusterContext>> instance) {
    super(instance);
  }

}
