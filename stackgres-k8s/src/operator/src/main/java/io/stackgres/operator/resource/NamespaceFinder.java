/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;

@ApplicationScoped
public class NamespaceFinder implements
    ResourceFinder<Namespace>,
    ResourceScanner<Namespace> {

  private final KubernetesClientFactory kubClientFactory;

  @Inject
  public NamespaceFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<Namespace> findByName(String name) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.namespaces().withName(name).get());
    }
  }

  @Override
  public List<Namespace> findResources() {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.namespaces().list().getItems();
    }
  }

}
