/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.quarkus.arc.Arc;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operatorframework.resource.AbstractResourceHandlerSelector;
import io.stackgres.operatorframework.resource.KindLiteral;
import io.stackgres.operatorframework.resource.ResourceHandler;

@ApplicationScoped
public class ClusterResourceHandlerSelector
    extends AbstractResourceHandlerSelector<StackGresClusterConfig> {

  private final Instance<ResourceHandler<StackGresClusterConfig>> handlers;

  @Inject
  public ClusterResourceHandlerSelector(
      @Any Instance<ResourceHandler<StackGresClusterConfig>> handlers) {
    this.handlers = handlers;
  }

  public ClusterResourceHandlerSelector() {
    if (Arrays.asList(new Exception().fillInStackTrace()
        .getStackTrace())
        .stream()
        .noneMatch(stackTraceElement -> stackTraceElement.getClassName()
            .equals(Arc.class.getName()))) {
      throw new IllegalStateException("Public no-args constructor can only be used from "
          + Arc.class.getName() + " class");
    }
    this.handlers = null;
  }

  @Override
  protected Stream<ResourceHandler<StackGresClusterConfig>> getResourceHandlers() {
    return handlers.stream();
  }

  @Override
  protected Optional<ResourceHandler<StackGresClusterConfig>> selectResourceHandler(
      KindLiteral kindLiteral) {
    Instance<ResourceHandler<StackGresClusterConfig>> instance = handlers.select(kindLiteral);
    return instance.isResolvable() ? Optional.of(instance.get()) : Optional.empty();
  }

  @Override
  protected Optional<ResourceHandler<StackGresClusterConfig>> getDefaultResourceHandler() {
    Instance<DefaultClusterResourceHandler> instance = handlers.select(
        DefaultClusterResourceHandler.class);
    return instance.isResolvable() ? Optional.of(instance.get()) : Optional.empty();
  }

}
