/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.StackGresContainer;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.SidecarLiteral;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.Envoy;

@ApplicationScoped
public class ClusterSidecarFinder implements SidecarFinder {

  private final Instance<StackGresClusterSidecarResourceFactory<?>> transformers;

  private final List<String> allSidecars;

  private final List<String> optionalSidecars;

  @Inject
  public ClusterSidecarFinder(
      @Any Instance<StackGresClusterSidecarResourceFactory<?>> transformers) {
    this.transformers = transformers;

    allSidecars = transformers.stream()
        .map(t -> findAnnotation(t, Sidecar.class))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Sidecar::value)
        .map(StackGresContainer::getName)
        .toList();

    String envoySidecarName = Envoy.class.getAnnotation(Sidecar.class).value().getName();

    optionalSidecars = allSidecars.stream().filter(s -> !s.equals(envoySidecarName))
        .toList();
  }

  @Override
  public StackGresClusterSidecarResourceFactory<?> getSidecarTransformer(
      String name) {
    Instance<StackGresClusterSidecarResourceFactory<?>> transformer = transformers
        .select(new SidecarLiteral(StackGresContainer.valueOf(name)));
    if (transformer.isResolvable()) {
      return transformer.get();
    }
    throw new IllegalStateException("Unknown sidecar with name " + name);
  }

  @Override
  public List<String> getAllOptionalSidecarNames() {
    return optionalSidecars;
  }

  @Override
  public List<String> getAllSidecars() {
    return allSidecars;
  }
}
