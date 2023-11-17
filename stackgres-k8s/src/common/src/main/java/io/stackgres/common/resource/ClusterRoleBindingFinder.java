/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRoleBindingFinder implements
    ResourceFinder<ClusterRoleBinding>,
    ResourceScanner<ClusterRoleBinding> {

  final KubernetesClient client;

  @Inject
  public ClusterRoleBindingFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<ClusterRoleBinding> findByName(String name) {
    return Optional.ofNullable(client.rbac().clusterRoleBindings()
        .withName(name).get());
  }

  @Override
  public Optional<ClusterRoleBinding> findByNameAndNamespace(String name, String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ClusterRoleBinding> findResources() {
    return client.rbac().clusterRoleBindings().list().getItems();
  }

  @Override
  public List<ClusterRoleBinding> findResourcesInNamespace(String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<ClusterRoleBinding> findByLabels(Map<String, String> labels) {
    return client.rbac().clusterRoleBindings().withLabels(labels).list().getItems();
  }

  @Override
  public List<ClusterRoleBinding> findByLabelsAndNamespace(
      String namespace, Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

}
