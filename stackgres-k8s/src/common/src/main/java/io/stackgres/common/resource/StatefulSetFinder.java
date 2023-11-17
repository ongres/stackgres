/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatefulSetFinder implements
    ResourceFinder<StatefulSet>,
    ResourceScanner<StatefulSet> {

  private KubernetesClient client;

  @Override
  public Optional<StatefulSet> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<StatefulSet> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.apps().statefulSets()
        .inNamespace(namespace).withName(name).get());
  }

  @Override
  public List<StatefulSet> findResources() {
    return client.apps().statefulSets().inAnyNamespace().list().getItems();
  }

  public List<StatefulSet> findResourcesWithLabels(Map<String, String> labels) {
    return client.apps().statefulSets()
        .inAnyNamespace().withLabels(labels).list().getItems();
  }

  @Override
  public List<StatefulSet> findResourcesInNamespace(String namespace) {
    return client.apps().statefulSets().inNamespace(namespace).list().getItems();
  }

  public List<StatefulSet> findResourcesInNamespaceWithLabels(String namespace,
      Map<String, String> labels) {
    return client.apps().statefulSets()
        .inNamespace(namespace).withLabels(labels).list().getItems();
  }

  @Inject
  public void setClient(KubernetesClient client) {
    this.client = client;
  }
}
