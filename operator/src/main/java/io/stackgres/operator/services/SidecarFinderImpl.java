/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.SidecarLiteral;
import io.stackgres.common.StackGresSidecarTransformer;

@ApplicationScoped
public class SidecarFinderImpl implements SidecarFinder {

  @Inject @Any
  Instance<StackGresSidecarTransformer<?>> transformers;

  @Override
  public StackGresSidecarTransformer<?> getSidecarTransformer(String name) {

    Instance<StackGresSidecarTransformer<?>> transformer = transformers
        .select(new SidecarLiteral(name));
    if (transformer.isResolvable()) {
      return transformer.get();
    }
    throw new IllegalStateException("Unknown sidecar with name " + name);

  }
}
