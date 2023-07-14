/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterHandlerDelegator implements HandlerDelegator<StackGresCluster> {

  private final Instance<ReconciliationHandler<StackGresCluster>> handlers;

  private final ReconciliationHandler<StackGresCluster> defaultHandler;

  @Inject
  public ClusterHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresCluster>> handlers,
      @ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresCluster> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresCluster context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresCluster context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresCluster context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresCluster context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresCluster> getHandler(HasMetadata r1) {
    Instance<ReconciliationHandler<StackGresCluster>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresCluster.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }

}
