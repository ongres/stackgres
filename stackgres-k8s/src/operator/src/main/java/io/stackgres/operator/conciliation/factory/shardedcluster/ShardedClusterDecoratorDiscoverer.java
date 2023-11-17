/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresShardedClusterContext> {

  @Inject
  public ShardedClusterDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresShardedClusterContext>> instance) {
    super(instance);
  }

}
