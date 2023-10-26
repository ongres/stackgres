/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.resource.AbstractUnamespacedResourceWriter;

@ApplicationScoped
public class NamespaceWriter
    extends AbstractUnamespacedResourceWriter<Namespace, Resource<Namespace>> {

  @Inject
  public NamespaceWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<
      Namespace,
      ?,
      Resource<Namespace>> getResourceEndpoints(KubernetesClient client) {
    return client.namespaces();
  }

}
