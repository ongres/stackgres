/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NamespaceFinder implements
    ResourceFinder<Namespace>,
    ResourceScanner<Namespace> {

  private final KubernetesClient client;

  @Inject
  public NamespaceFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<Namespace> findByName(String name) {
    return Optional.ofNullable(client.namespaces().withName(name).get());
  }

  @Override
  public Optional<Namespace> findByNameAndNamespace(String name, String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Namespace> findResources() {
    return client.namespaces().list().getItems();
  }

  @Override
  public List<Namespace> findResourcesInNamespace(String namespace) {
    throw new UnsupportedOperationException();
  }

}
