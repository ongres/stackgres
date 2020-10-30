/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operatorframework.reconciliation.ResourceGeneratorReconciliationCycle;
import io.stackgres.operatorframework.reconciliation.ResourceGeneratorReconciliator;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;

public abstract class StackGresReconciliationCycle<T extends ResourceHandlerContext,
    H extends CustomResource, S extends ResourceHandlerSelector<T>>
    extends ResourceGeneratorReconciliationCycle<T, H, S> {

  private static final String STACKGRES_IO_RECONCILIATION = "stackgres.io/reconciliation-pause";

  private final CustomResourceScanner<H> clusterScanner;

  protected StackGresReconciliationCycle(String name,
      Supplier<KubernetesClient> clientSupplier,
      ResourceGeneratorReconciliator<T, H, S> reconciliator,
      Function<T, H> resourceGetter,
      S handlerSelector,
      CustomResourceScanner<H> clusterScanner) {
    super(name, clientSupplier, reconciliator, resourceGetter, handlerSelector);
    this.clusterScanner = clusterScanner;
  }

  @Override
  protected ImmutableList<T> getExistingContexts() {
    return clusterScanner.getResources()
        .stream().filter(r -> Optional.ofNullable(r.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(STACKGRES_IO_RECONCILIATION))
            .map(Boolean::parseBoolean)
            .map(b -> !b)
            .orElse(true))
        .map(this::mapResourceToContext)
        .collect(ImmutableList.toImmutableList());
  }

  protected abstract T mapResourceToContext(H resource);

}
