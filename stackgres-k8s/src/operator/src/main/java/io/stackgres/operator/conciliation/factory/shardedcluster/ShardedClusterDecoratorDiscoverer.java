/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;

@ApplicationScoped
public class ShardedClusterDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresShardedClusterContext> {

  @Inject
  public ShardedClusterDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresShardedClusterContext>> instance) {
    super(instance);
  }

}
