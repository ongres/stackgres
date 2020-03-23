/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;

@ApplicationScoped
public class SecretFinder implements
    ResourceFinder<Secret>,
    ResourceScanner<Secret> {

  private final KubernetesClientFactory kubClientFactory;

  @Inject
  public SecretFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<Secret> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Secret> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(name).get());
    }
  }

  @Override
  public List<Secret> findResources() {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.secrets().inAnyNamespace().list().getItems();
    }
  }

  @Override
  public List<Secret> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.secrets().inNamespace(namespace).list().getItems();
    }
  }

}
