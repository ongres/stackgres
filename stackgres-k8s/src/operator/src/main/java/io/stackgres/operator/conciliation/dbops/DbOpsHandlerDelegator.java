/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class DbOpsHandlerDelegator implements HandlerDelegator<StackGresDbOps> {

  private final Instance<ReconciliationHandler<StackGresDbOps>> handlers;

  private final ReconciliationHandler<StackGresDbOps> defaultHandler;

  @Inject
  public DbOpsHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresDbOps>> handlers,
      @ReconciliationScope(value = StackGresDbOps.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresDbOps> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresDbOps context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresDbOps context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresDbOps context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresDbOps context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresDbOps> getHandler(HasMetadata r1) {
    Instance<ReconciliationHandler<StackGresDbOps>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresCluster.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }
}
