/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.handler;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;

@ApplicationScoped
public class DbOpsManagedPodHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null && context.isDbOpsPod(resource);
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}
