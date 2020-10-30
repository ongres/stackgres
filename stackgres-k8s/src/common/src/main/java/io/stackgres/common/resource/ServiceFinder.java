/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;

@ApplicationScoped
public class ServiceFinder implements
    ResourceFinder<Service>,
    ResourceScanner<Service> {

  final KubernetesClientFactory kubClientFactory;

  @Inject
  public ServiceFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<Service> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Service> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.services().inNamespace(namespace).withName(name).get());
    }
  }

  @Override
  public List<Service> findResources() {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.services().inAnyNamespace().list().getItems();
    }
  }

  @Override
  public List<Service> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.services().inNamespace(namespace).list().getItems();
    }
  }

}
