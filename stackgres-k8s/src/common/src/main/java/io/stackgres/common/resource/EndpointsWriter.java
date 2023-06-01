/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class EndpointsWriter
    extends AbstractResourceWriter<Endpoints, EndpointsList, Resource<Endpoints>> {

  @Inject
  public EndpointsWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<Endpoints, EndpointsList, Resource<Endpoints>>
      getResourceEndpoints(KubernetesClient client) {
    return client.endpoints();
  }

}
