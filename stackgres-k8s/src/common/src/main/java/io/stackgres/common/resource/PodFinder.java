/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PodFinder implements
    ResourceFinder<Pod>,
    ResourceScanner<Pod> {

  private KubernetesClient client;

  @Override
  public Optional<Pod> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Pod> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.pods().inNamespace(namespace).withName(name).get());
  }

  @Override
  public List<Pod> findResources() {
    return client.pods().inAnyNamespace().list().getItems();
  }

  public List<Pod> findResourcesWithLabels(Map<String, String> labels) {
    return client.pods().inAnyNamespace().withLabels(labels).list().getItems();
  }

  @Override
  public List<Pod> findResourcesInNamespace(String namespace) {
    return client.pods().inNamespace(namespace).list().getItems();
  }

  @Override
  public List<Pod> findByLabelsAndNamespace(String namespace, Map<String, String> labels) {
    return client.pods().inNamespace(namespace).withLabels(labels).list().getItems().stream()
        .collect(Collectors.toUnmodifiableList());
  }

  @Inject
  public void setClient(KubernetesClient client) {
    this.client = client;
  }
}
