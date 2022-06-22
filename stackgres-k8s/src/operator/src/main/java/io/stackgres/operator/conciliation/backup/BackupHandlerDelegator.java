/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class BackupHandlerDelegator implements HandlerDelegator<StackGresBackup> {

  private final Instance<ReconciliationHandler<StackGresBackup>> handlers;

  private final ReconciliationHandler<StackGresBackup> defaultHandler;

  @Inject
  public BackupHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresBackup>> handlers,
      @ReconciliationScope(value = StackGresBackup.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresBackup> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresBackup context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresBackup context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresBackup context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresBackup context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresBackup> getHandler(HasMetadata resource) {
    Instance<ReconciliationHandler<StackGresBackup>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresBackup.class, resource.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }
}
