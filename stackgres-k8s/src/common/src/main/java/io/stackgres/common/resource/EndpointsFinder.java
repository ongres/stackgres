/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class EndpointsFinder implements
    ResourceFinder<Endpoints>,
    ResourceScanner<Endpoints> {

  private KubernetesClient client;

  @Override
  public Optional<Endpoints> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Endpoints> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.endpoints().inNamespace(namespace).withName(name).get());
  }

  @Override
  public List<Endpoints> findResources() {
    return client.endpoints().inAnyNamespace().list().getItems();
  }

  public List<Endpoints> findResourcesWithLabels(Map<String, String> labels) {
    return client.endpoints().inAnyNamespace().withLabels(labels).list().getItems();
  }

  @Override
  public List<Endpoints> findResourcesInNamespace(String namespace) {
    return client.endpoints().inNamespace(namespace).list().getItems();
  }

  @Override
  public List<Endpoints> findByLabelsAndNamespace(String namespace, Map<String, String> labels) {
    return client.endpoints().inNamespace(namespace).withLabels(labels).list().getItems().stream()
        .collect(Collectors.toUnmodifiableList());
  }

  @Inject
  public void setClient(KubernetesClient client) {
    this.client = client;
  }
}
