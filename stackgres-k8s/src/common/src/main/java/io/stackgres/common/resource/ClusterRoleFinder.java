/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRoleFinder implements
    ResourceFinder<ClusterRole>,
    ResourceScanner<ClusterRole> {

  final KubernetesClient client;

  @Inject
  public ClusterRoleFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<ClusterRole> findByName(String name) {
    return Optional.ofNullable(client.rbac().clusterRoles()
        .withName(name).get());
  }

  @Override
  public Optional<ClusterRole> findByNameAndNamespace(String name, String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ClusterRole> findResources() {
    return client.rbac().clusterRoles().list().getItems();
  }

  @Override
  public List<ClusterRole> findResourcesInNamespace(String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ClusterRole> findByLabels(Map<String, String> labels) {
    return client.rbac().clusterRoles().withLabels(labels).list().getItems();
  }

  @Override
  public List<ClusterRole> findByLabelsAndNamespace(
      String namespace, Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

}
