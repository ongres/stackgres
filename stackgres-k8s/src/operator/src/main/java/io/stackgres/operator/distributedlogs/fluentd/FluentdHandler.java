/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.distributedlogs.fluentd;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operator.resource.AbstractDistributedLogsResourceHandler;

@ApplicationScoped
public class FluentdHandler extends AbstractDistributedLogsResourceHandler {

  @Override
  public boolean isHandlerForResource(
      StackGresDistributedLogsContext context, HasMetadata resource) {
    return context != null
        && resource instanceof Endpoints
        && resource.getMetadata().getNamespace().equals(
            context.getDistributedLogs().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            Fluentd.serviceName(context));
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}
