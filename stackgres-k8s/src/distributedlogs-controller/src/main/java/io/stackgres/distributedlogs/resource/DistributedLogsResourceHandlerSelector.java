/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.resource;

import java.util.Optional;
import java.util.stream.Stream;

import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.AbstractResourceHandlerSelector;
import io.stackgres.operatorframework.resource.ResourceHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DistributedLogsResourceHandlerSelector
    extends AbstractResourceHandlerSelector<StackGresDistributedLogsContext> {

  private final Instance<ResourceHandler<StackGresDistributedLogsContext>> handlers;

  @Inject
  public DistributedLogsResourceHandlerSelector(
      @Any Instance<ResourceHandler<StackGresDistributedLogsContext>> handlers) {
    this.handlers = handlers;
  }

  @Override
  protected Stream<ResourceHandler<StackGresDistributedLogsContext>> getResourceHandlers() {
    return Seq.seq(handlers);
  }

  @Override
  protected Optional<ResourceHandler<StackGresDistributedLogsContext>> getDefaultResourceHandler() {
    Instance<DefaultDistributedLogsResourceHandler> instance = handlers.select(
        DefaultDistributedLogsResourceHandler.class);
    return instance.isResolvable()
        ? Optional.of(instance.get())
        : Optional.empty();
  }

}
