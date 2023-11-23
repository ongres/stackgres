/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.resource.AbstractUnamespacedResourceWriter;
import io.stackgres.common.resource.ResourceFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class CustomResourceDefinitionFinder
    extends AbstractUnamespacedResourceWriter<
        CustomResourceDefinition, Resource<CustomResourceDefinition>>
    implements ResourceFinder<CustomResourceDefinition> {

  private final KubernetesClient client;

  @Inject
  public CustomResourceDefinitionFinder(KubernetesClient client) {
    super(client);
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
  public void delete(
      CustomResourceDefinition resource,
      boolean dryRun) {
    throw new UnsupportedOperationException("CustomResourceDefinition deletion is not supported");
  }

  @Override
  protected NonNamespaceOperation<
          CustomResourceDefinition,
          ?,
          Resource<CustomResourceDefinition>> getResourceEndpoints(
      KubernetesClient client) {
    return client.apiextensions().v1().customResourceDefinitions();
  }

}
