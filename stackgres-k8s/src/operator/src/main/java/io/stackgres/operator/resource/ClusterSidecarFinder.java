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
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.SidecarLiteral;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresSidecarTransformer;
import io.stackgres.operator.sidecars.envoy.Envoy;

@ApplicationScoped
public class ClusterSidecarFinder implements SidecarFinder<StackGresClusterContext> {

  private final Instance<StackGresSidecarTransformer<?, StackGresClusterContext>> transformers;

  private final List<String> allSidecars;

  @Inject
  public ClusterSidecarFinder(
      @Any Instance<StackGresSidecarTransformer<?, StackGresClusterContext>> transformers) {
    this.transformers = transformers;
    allSidecars = transformers.stream()
        .filter((t) -> {
          Class<? extends StackGresSidecarTransformer> transformerClass = t.getClass();
          String envoySidecarName = Envoy.class.getAnnotation(Sidecar.class).value();
          return transformerClass.isAnnotationPresent(Sidecar.class)
              && !transformerClass.getAnnotation(Sidecar.class).value().equals(envoySidecarName);
        })
        .map(t -> t.getClass().getAnnotation(Sidecar.class).value())
        .collect(ImmutableList.toImmutableList());
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

  @Override
  public List<String> getAllOptionalSidecarNames() {
    return allSidecars;
  }

}
