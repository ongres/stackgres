/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ServiceFinder extends AbstractResourceFinderAndScanner<Service> {

  @Inject
  public ServiceFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<Service, ? extends KubernetesResourceList<Service>, ? extends Resource<Service>>
      getOperation(KubernetesClient client) {
    return client.services();
  }

}
