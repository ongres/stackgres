/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamHandlerDelegator implements HandlerDelegator<StackGresStream> {

  private final Instance<ReconciliationHandler<StackGresStream>> handlers;

  private final ReconciliationHandler<StackGresStream> defaultHandler;

  @Inject
  public StreamHandlerDelegator(
      @Any Instance<ReconciliationHandler<StackGresStream>> handlers,
      @ReconciliationScope(value = StackGresStream.class, kind = "HasMetadata")
          ReconciliationHandler<StackGresStream> defaultHandler) {
    this.handlers = handlers;
    this.defaultHandler = defaultHandler;
  }

  @Override
  public HasMetadata create(StackGresStream context, HasMetadata resource) {
    return getHandler(resource).create(context, resource);
  }

  @Override
  public HasMetadata patch(StackGresStream context, HasMetadata newResource,
      HasMetadata oldResource) {
    return getHandler(newResource).patch(context, newResource, oldResource);
  }

  @Override
  public HasMetadata replace(StackGresStream context, HasMetadata resource) {
    return getHandler(resource).replace(context, resource);
  }

  @Override
  public void delete(StackGresStream context, HasMetadata resource) {
    getHandler(resource).delete(context, resource);
  }

  private ReconciliationHandler<StackGresStream> getHandler(HasMetadata resource) {
    Instance<ReconciliationHandler<StackGresStream>> instance = handlers
        .select(new ReconciliationScopeLiteral(StackGresStream.class, resource.getKind()));
    if (!instance.isResolvable()) {
      return defaultHandler;
    } else {
      return instance.get();
    }
  }
}
