/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;

public abstract class ResourceGeneratorReconciliationCycle<
      T extends ResourceHandlerContext,
      H extends CustomResource<?, ?>,
      S extends ResourceHandlerSelector<T>>
    extends ReconciliationCycle<T, H, S> {

  protected ResourceGeneratorReconciliationCycle(String name,
      KubernetesClient client,
      ResourceGeneratorReconciliator<T, H, S> reconciliator,
      S handlerSelector) {
    super(name, client, reconciliator, handlerSelector);
  }

}
