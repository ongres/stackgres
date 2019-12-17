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
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.common.StackGresSidecarTransformer;

@ApplicationScoped
public class ClusterSidecarFinder implements SidecarFinder<StackGresClusterConfig> {

  private final Instance<StackGresSidecarTransformer<?, StackGresClusterConfig>> transformers;

  @Inject
  public ClusterSidecarFinder(
      @Any Instance<StackGresSidecarTransformer<?, StackGresClusterConfig>> transformers) {
    this.transformers = transformers;
  }

  @Override
  public StackGresSidecarTransformer<?, StackGresClusterConfig> getSidecarTransformer(String name) {
    Instance<StackGresSidecarTransformer<?, StackGresClusterConfig>> transformer = transformers
        .select(new SidecarLiteral(name));
    if (transformer.isResolvable()) {
      return transformer.get();
    }
    throw new IllegalStateException("Unknown sidecar with name " + name);
  }

}
