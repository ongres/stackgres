/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliationCycle;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;

public abstract class StackGresReconciliationCycle<T extends ResourceHandlerContext,
    H extends CustomResource, S extends ResourceHandlerSelector<T>>
    extends AbstractReconciliationCycle<T, H, S> {

  private final CustomResourceScanner<H> clusterScanner;

  protected StackGresReconciliationCycle(String name,
                                         Supplier<KubernetesClient> clientSupplier,
                                         Function<T, H> resourceGetter,
                                         S handlerSelector,
                                         ObjectMapper objectMapper,
                                         CustomResourceScanner<H> clusterScanner) {
    super(name, clientSupplier, resourceGetter, handlerSelector, objectMapper);
    this.clusterScanner = clusterScanner;
  }

  @Override
  protected ImmutableList<T> getExistingConfigs() {
    return clusterScanner.getResources()
        .stream().filter(r -> Optional.ofNullable(r.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(StackGresContext.RECONCILIATION_PAUSE_KEY))
            .map(Boolean::parseBoolean)
            .map(b -> !b)
            .orElse(true))
        .map(this::mapResourceToContext)
        .collect(ImmutableList.toImmutableList());
  }

  protected abstract T mapResourceToContext(H resource);

}
