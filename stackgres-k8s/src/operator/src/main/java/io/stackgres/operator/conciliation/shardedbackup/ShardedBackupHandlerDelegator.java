/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class ShardedBackupHandlerDelegator implements HandlerDelegator<StackGresShardedBackup> {

  private final Instance<ReconciliationHandler<StackGresShardedBackup>> handlers;

  private final ReconciliationHandler<StackGresShardedBackup> defaultHandler;

  @Inject
  public ShardedBackupHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresShardedBackup>> handlers,
      @ReconciliationScope(value = StackGresShardedBackup.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresShardedBackup> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresShardedBackup context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresShardedBackup context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresShardedBackup context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresShardedBackup context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresShardedBackup> getHandler(HasMetadata resource) {
    Instance<ReconciliationHandler<StackGresShardedBackup>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresShardedBackup.class, resource.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }
}
