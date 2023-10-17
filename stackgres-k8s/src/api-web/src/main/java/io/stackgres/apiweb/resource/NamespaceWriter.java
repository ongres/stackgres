/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.ResourceWriter;

@ApplicationScoped
public class NamespaceWriter implements ResourceWriter<Namespace> {

  private final KubernetesClient client;

  @Inject
  public NamespaceWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Namespace create(Namespace resource) {
    return client.namespaces()
        .resource(resource)
        .create();
  }

  @Override
  public Namespace update(Namespace resource) {
    return client.namespaces()
        .resource(resource)
        .patch();
  }

  @Override
  public Namespace update(Namespace resource, String patch) {
    return client.namespaces()
        .resource(resource)
        .patch(patch);
  }

  @Override
  public void delete(Namespace resource) {
    client.namespaces()
        .resource(resource)
        .delete();
  }

}
