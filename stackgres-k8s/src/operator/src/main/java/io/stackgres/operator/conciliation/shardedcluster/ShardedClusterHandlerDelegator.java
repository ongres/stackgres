/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterHandlerDelegator implements HandlerDelegator<StackGresShardedCluster> {

  private final Instance<ReconciliationHandler<StackGresShardedCluster>> handlers;

  private final ReconciliationHandler<StackGresShardedCluster> defaultHandler;

  @Inject
  public ShardedClusterHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresShardedCluster>> handlers,
      @ReconciliationScope(value = StackGresShardedCluster.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresShardedCluster> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresShardedCluster context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresShardedCluster context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresShardedCluster context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresShardedCluster context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresShardedCluster> getHandler(HasMetadata r1) {
    Instance<ReconciliationHandler<StackGresShardedCluster>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresShardedCluster.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }

}
