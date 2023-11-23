/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedDbOpsDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresShardedDbOpsContext> {

  @Inject
  public ShardedDbOpsDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresShardedDbOpsContext>> instance) {
    super(instance);
  }

}
