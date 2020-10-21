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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class DistributedLogsHandlerDelegator implements HandlerDelegator<StackGresDistributedLogs> {

  private final Instance<ReconciliationHandler> handlers;

  private final ReconciliationHandler defaultHandler;

  @Inject
  public DistributedLogsHandlerDelegator(
      @Any Instance<ReconciliationHandler> handlers,
      @ReconciliationScope(value = StackGresDistributedLogs.class, kind = "HasMetadata")
          ReconciliationHandler defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(HasMetadata resource) {
    return getHandler(resource).create(resource);
  }

  @Override
  public HasMetadata patch(HasMetadata resource) {
    return getHandler(resource).patch(resource);
  }

  @Override
  public void delete(HasMetadata resource) {
    getHandler(resource).delete(resource);
  }

  private ReconciliationHandler getHandler(HasMetadata r1) {
    Instance<ReconciliationHandler> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresDistributedLogs.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }
}
