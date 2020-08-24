/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;

@ApplicationScoped
public class PodFinder implements
    ResourceFinder<Pod>,
    ResourceScanner<Pod> {

  private KubernetesClientFactory kubClientFactory;

  @Override
  public Optional<Pod> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Pod> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.pods().inNamespace(namespace).withName(name).get());
    }
  }

  @Override
  public List<Pod> findResources() {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.pods().inAnyNamespace().list().getItems();
    }
  }

  public List<Pod> findResourcesWithLabels(Map<String, String> labels) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.pods().inAnyNamespace().withLabels(labels).list().getItems();
    }
  }

  @Override
  public List<Pod> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.pods().inNamespace(namespace).list().getItems();
    }
  }

  public List<Pod> findResourcesInNamespaceWithLabels(String namespace,
      Map<String, String> labels) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.pods().inNamespace(namespace).withLabels(labels).list().getItems();
    }
  }

  @Inject
  public void setKubClientFactory(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }
}
