/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigHandlerDelegator implements HandlerDelegator<StackGresConfig> {

  private final Instance<ReconciliationHandler<StackGresConfig>> handlers;

  private final ReconciliationHandler<StackGresConfig> defaultHandler;

  @Inject
  public ConfigHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresConfig>> handlers,
      @ReconciliationScope(value = StackGresConfig.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresConfig> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresConfig context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresConfig context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresConfig context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresConfig context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresConfig> getHandler(HasMetadata r1) {
    Instance<ReconciliationHandler<StackGresConfig>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresConfig.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }

}
