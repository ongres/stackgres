/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoleBindingFinder implements
    ResourceFinder<RoleBinding>,
    ResourceScanner<RoleBinding> {

  final KubernetesClient client;

  @Inject
  public RoleBindingFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<RoleBinding> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<RoleBinding> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.rbac().roleBindings()
        .inNamespace(namespace).withName(name).get());
  }

  @Override
  public List<RoleBinding> findResources() {
    return client.rbac().roleBindings().inAnyNamespace().list().getItems();
  }

  @Override
  public List<RoleBinding> findResourcesInNamespace(String namespace) {
    return client.rbac().roleBindings().inNamespace(namespace).list().getItems();
  }

  @Override
  public List<RoleBinding> findByLabels(Map<String, String> labels) {
    return client.rbac().roleBindings().inAnyNamespace().withLabels(labels).list().getItems();
  }

  @Override
  public List<RoleBinding> findByLabelsAndNamespace(String namespace, Map<String, String> labels) {
    return client.rbac().roleBindings().inNamespace(namespace).withLabels(labels).list().getItems();
  }

}
