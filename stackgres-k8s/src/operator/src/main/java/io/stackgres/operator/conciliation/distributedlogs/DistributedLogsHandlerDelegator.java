/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class DistributedLogsHandlerDelegator implements HandlerDelegator<StackGresDistributedLogs> {

  private final Instance<ReconciliationHandler<StackGresDistributedLogs>> handlers;

  private final ReconciliationHandler<StackGresDistributedLogs> defaultHandler;

  @Inject
  public DistributedLogsHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresDistributedLogs>> handlers,
      @ReconciliationScope(value = StackGresDistributedLogs.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresDistributedLogs> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresDistributedLogs context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresDistributedLogs context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresDistributedLogs context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresDistributedLogs context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresDistributedLogs> getHandler(HasMetadata r1) {
    Instance<ReconciliationHandler<StackGresDistributedLogs>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresDistributedLogs.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }
}
