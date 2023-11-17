/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedBackupDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresShardedBackupContext> {

  @Inject
  public ShardedBackupDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresShardedBackupContext>> instance) {
    super(instance);
  }

}
