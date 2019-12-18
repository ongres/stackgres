/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.SidecarLiteral;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresSidecarTransformer;

@ApplicationScoped
public class ClusterSidecarFinder implements SidecarFinder<StackGresClusterContext> {

  private final Instance<StackGresSidecarTransformer<?, StackGresClusterContext>> transformers;

  @Inject
  public ClusterSidecarFinder(
      @Any Instance<StackGresSidecarTransformer<?, StackGresClusterContext>> transformers) {
    this.transformers = transformers;
  }

  @Override
  public StackGresSidecarTransformer<?, StackGresClusterContext> getSidecarTransformer(
      String name) {
    Instance<StackGresSidecarTransformer<?, StackGresClusterContext>> transformer = transformers
        .select(new SidecarLiteral(name));
    if (transformer.isResolvable()) {
      return transformer.get();
    }
    throw new IllegalStateException("Unknown sidecar with name " + name);
  }

}
