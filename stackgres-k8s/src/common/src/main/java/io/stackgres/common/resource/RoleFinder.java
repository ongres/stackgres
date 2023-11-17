/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoleFinder implements
    ResourceFinder<Role>,
    ResourceScanner<Role> {

  final KubernetesClient client;

  @Inject
  public RoleFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<Role> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Role> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.rbac().roles().inNamespace(namespace).withName(name).get());
  }

  @Override
  public List<Role> findResources() {
    return client.rbac().roles().inAnyNamespace().list().getItems();
  }

  @Override
  public List<Role> findResourcesInNamespace(String namespace) {
    return client.rbac().roles().inNamespace(namespace).list().getItems();
  }

  @Override
  public List<Role> findByLabels(Map<String, String> labels) {
    return client.rbac().roles().inAnyNamespace().withLabels(labels).list().getItems();
  }

  @Override
  public List<Role> findByLabelsAndNamespace(String namespace, Map<String, String> labels) {
    return client.rbac().roles().inNamespace(namespace).withLabels(labels).list().getItems();
  }

}
