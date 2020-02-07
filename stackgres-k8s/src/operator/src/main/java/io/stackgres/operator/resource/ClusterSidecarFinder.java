/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import io.stackgres.operator.cluster.sidecars.envoy.Envoy;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.SidecarLiteral;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;

@ApplicationScoped
public class ClusterSidecarFinder implements SidecarFinder {

  private final Instance<StackGresClusterSidecarResourceFactory<?>> transformers;

  private final List<String> allSidecars;

  @Inject
  public ClusterSidecarFinder(
      @Any Instance<StackGresClusterSidecarResourceFactory<?>> transformers) {
    this.transformers = transformers;
    allSidecars = transformers.stream()
        .filter(transformer -> {
          String envoySidecarName = Envoy.class.getAnnotation(Sidecar.class).value();
          return transformer.getClass().isAnnotationPresent(Sidecar.class)
              && !transformer.getClass().getAnnotation(Sidecar.class).value()
              .equals(envoySidecarName);
        })
        .map(transformer -> transformer.getClass().getAnnotation(Sidecar.class).value())
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public StackGresClusterSidecarResourceFactory<?> getSidecarTransformer(
      String name) {
    Instance<StackGresClusterSidecarResourceFactory<?>> transformer = transformers
        .select(new SidecarLiteral(name));
    if (transformer.isResolvable()) {
      return transformer.get();
    }
    throw new IllegalStateException("Unknown sidecar with name " + name);
  }

  @Override
  public List<String> getAllOptionalSidecarNames() {
    return allSidecars;
  }

}
