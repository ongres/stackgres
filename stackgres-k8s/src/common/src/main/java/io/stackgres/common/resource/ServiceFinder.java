/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ServiceFinder implements
    ResourceFinder<Service>,
    ResourceScanner<Service> {

  final KubernetesClient client;

  @Inject
  public ServiceFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<Service> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Service> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.services().inNamespace(namespace).withName(name).get());
  }

  @Override
  public List<Service> findResources() {
    return client.services().inAnyNamespace().list().getItems();
  }

  @Override
  public List<Service> findResourcesInNamespace(String namespace) {
    return client.services().inNamespace(namespace).list().getItems();
  }

}
