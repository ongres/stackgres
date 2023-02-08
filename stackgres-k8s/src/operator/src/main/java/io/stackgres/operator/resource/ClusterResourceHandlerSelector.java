/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.AbstractResourceHandlerSelector;
import io.stackgres.operatorframework.resource.ResourceHandler;

@ApplicationScoped
public class ClusterResourceHandlerSelector
    extends AbstractResourceHandlerSelector<StackGresClusterContext> {

  private final Instance<ResourceHandler<StackGresClusterContext>> handlers;

  @Inject
  public ClusterResourceHandlerSelector(
      @Any Instance<ResourceHandler<StackGresClusterContext>> handlers) {
    this.handlers = handlers;
  }

  @Override
  protected Stream<ResourceHandler<StackGresClusterContext>>
      getResourceHandlers() {
    return handlers.stream();
  }

  @Override
  protected Optional<ResourceHandler<StackGresClusterContext>>
      getDefaultResourceHandler() {
    Instance<DefaultClusterResourceHandler> instance = handlers.select(
        DefaultClusterResourceHandler.class);
    return instance.isResolvable() ? Optional.of(instance.get()) : Optional.empty();
  }

}
