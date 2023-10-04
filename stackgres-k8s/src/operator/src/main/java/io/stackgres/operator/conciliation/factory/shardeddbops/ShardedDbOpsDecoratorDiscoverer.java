/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;

@ApplicationScoped
public class ShardedDbOpsDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresShardedDbOpsContext> {

  @Inject
  public ShardedDbOpsDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresShardedDbOpsContext>> instance) {
    super(instance);
  }

}
