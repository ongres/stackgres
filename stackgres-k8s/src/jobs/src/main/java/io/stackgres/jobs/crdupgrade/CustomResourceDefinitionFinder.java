/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;

public class CustomResourceDefinitionFinder implements ResourceFinder<CustomResourceDefinition>,
    ResourceWriter<CustomResourceDefinition> {

  private final KubernetesClient client;

  public CustomResourceDefinitionFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public @NotNull Optional<CustomResourceDefinition> findByName(String name) {
    return Optional.ofNullable(client.apiextensions().v1()
        .customResourceDefinitions()
        .withName(name)
        .get());
  }

  @Override
  public @NotNull Optional<CustomResourceDefinition> findByNameAndNamespace(
      String name, String namespace) {
    return findByName(name);
  }

  @Override
  public CustomResourceDefinition create(CustomResourceDefinition resource) {
    return client.apiextensions().v1()
        .customResourceDefinitions()
        .resource(resource)
        .create();
  }

  @Override
  public CustomResourceDefinition update(CustomResourceDefinition resource) {
    return client.apiextensions().v1()
        .customResourceDefinitions()
        .withName(resource.getMetadata().getName())
        .patch(resource);
  }

  @Override
  public void delete(CustomResourceDefinition resource) {
    throw new UnsupportedOperationException("Custom Resource deletion not supported");

  }
}
