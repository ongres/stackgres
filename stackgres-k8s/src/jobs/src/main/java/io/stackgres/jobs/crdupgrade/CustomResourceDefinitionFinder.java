/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;

public class CustomResourceDefinitionFinder implements ResourceFinder<CustomResourceDefinition>,
    ResourceWriter<CustomResourceDefinition> {

  private final KubernetesClientFactory kubernetesClientFactory;

  public CustomResourceDefinitionFinder(KubernetesClientFactory kubernetesClientFactory) {
    this.kubernetesClientFactory = kubernetesClientFactory;
  }

  @Override
  public Optional<CustomResourceDefinition> findByName(String name) {

    try (KubernetesClient client = kubernetesClientFactory.create()) {
      return Optional.ofNullable(client.apiextensions().v1()
          .customResourceDefinitions()
          .withName(name)
          .get());
    }

  }

  @Override
  public Optional<CustomResourceDefinition> findByNameAndNamespace(String name, String namespace) {
    return findByName(name);
  }

  @Override
  public void create(CustomResourceDefinition resource) {
    try (KubernetesClient client = kubernetesClientFactory.create()) {
      client.apiextensions().v1()
          .customResourceDefinitions()
          .create(resource);
    }
  }

  @Override
  public void update(CustomResourceDefinition resource) {
    try (KubernetesClient client = kubernetesClientFactory.create()) {
      client.apiextensions().v1()
          .customResourceDefinitions()
          .withName(resource.getMetadata().getName())
          .patch(resource);
    }
  }

  @Override
  public void delete(CustomResourceDefinition resource) {
    throw new UnsupportedOperationException("Custom Resource deletion not supported");

  }
}
