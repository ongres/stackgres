/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.resource;

import java.util.Optional;
import java.util.stream.Stream;

import io.stackgres.operatorframework.resource.AbstractResourceHandlerSelector;
import io.stackgres.operatorframework.resource.ResourceHandler;
import io.stackgres.stream.common.StackGresStreamContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class StreamResourceHandlerSelector
    extends AbstractResourceHandlerSelector<StackGresStreamContext> {

  private final Instance<ResourceHandler<StackGresStreamContext>> handlers;

  @Inject
  public StreamResourceHandlerSelector(
      @Any Instance<ResourceHandler<StackGresStreamContext>> handlers) {
    this.handlers = handlers;
  }

  @Override
  protected Stream<ResourceHandler<StackGresStreamContext>> getResourceHandlers() {
    return Seq.seq(handlers);
  }

  @Override
  protected Optional<ResourceHandler<StackGresStreamContext>> getDefaultResourceHandler() {
    Instance<DefaultStreamResourceHandler> instance = handlers.select(DefaultStreamResourceHandler.class);
    return instance.isResolvable()
        ? Optional.of(instance.get())
        : Optional.empty();
  }

}
