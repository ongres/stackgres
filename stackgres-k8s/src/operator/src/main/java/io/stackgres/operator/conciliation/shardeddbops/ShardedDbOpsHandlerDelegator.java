/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class ShardedDbOpsHandlerDelegator implements HandlerDelegator<StackGresShardedDbOps> {

  private final Instance<ReconciliationHandler<StackGresShardedDbOps>> handlers;

  private final ReconciliationHandler<StackGresShardedDbOps> defaultHandler;

  @Inject
  public ShardedDbOpsHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresShardedDbOps>> handlers,
      @ReconciliationScope(value = StackGresShardedDbOps.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresShardedDbOps> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresShardedDbOps context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresShardedDbOps context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresShardedDbOps context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresShardedDbOps context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresShardedDbOps> getHandler(HasMetadata resource) {
    Instance<ReconciliationHandler<StackGresShardedDbOps>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresShardedDbOps.class, resource.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }
}
