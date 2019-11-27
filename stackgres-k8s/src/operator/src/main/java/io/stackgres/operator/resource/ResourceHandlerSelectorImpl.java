/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.KindLiteral;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.controller.ResourceHandlerContext;

@ApplicationScoped
public class ResourceHandlerSelectorImpl implements ResourceHandlerSelector {

  private Instance<ResourceHandler> handlers;

  @Inject
  public ResourceHandlerSelectorImpl(@Any Instance<ResourceHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public boolean equals(ResourceHandlerContext resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return selectResourceHandler(resourceHandlerContext.getClusterConfig(), requiredResource)
        .equals(resourceHandlerContext, existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(ResourceHandlerContext resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return selectResourceHandler(resourceHandlerContext.getClusterConfig(), requiredResource)
        .update(resourceHandlerContext, existingResource, requiredResource);
  }

  @Override
  public boolean isManaged(StackGresClusterConfig config, HasMetadata existingResource) {
    return selectResourceHandler(config, existingResource).isManaged();
  }

  @Override
  public boolean skipCreation(StackGresClusterConfig config, HasMetadata requiredResource) {
    return selectResourceHandler(config, requiredResource).skipCreation();
  }

  @Override
  public boolean skipDeletion(StackGresClusterConfig config, HasMetadata requiredResource) {
    return selectResourceHandler(config, requiredResource).skipDeletion();
  }

  @Override
  public void registerKinds() {
    handlers.stream()
        .forEach(ResourceHandler::registerKind);
  }

  @Override
  public Stream<HasMetadata> getOrphanResources(KubernetesClient client,
      ImmutableList<StackGresClusterConfig> existingConfigs) {
    return handlers.stream()
        .flatMap(handler -> handler.getOrphanResources(client, existingConfigs));
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client, StackGresClusterConfig config) {
    return handlers.stream()
        .flatMap(handler -> handler.getResources(client, config));
  }

  @Override
  public Optional<HasMetadata> find(KubernetesClient client, StackGresClusterConfig config,
      HasMetadata resource) {
    return selectResourceHandler(config, resource).find(client, resource);
  }

  @Override
  public HasMetadata create(KubernetesClient client, StackGresClusterConfig config,
      HasMetadata resource) {
    return selectResourceHandler(config, resource).create(client, resource);
  }

  @Override
  public HasMetadata patch(KubernetesClient client, StackGresClusterConfig config,
      HasMetadata resource) {
    return selectResourceHandler(config, resource).patch(client, resource);
  }

  @Override
  public boolean delete(KubernetesClient client, StackGresClusterConfig config,
      HasMetadata resource) {
    return selectResourceHandler(config, resource).delete(client, resource);
  }

  private ResourceHandler selectResourceHandler(StackGresClusterConfig config,
      HasMetadata resource) {
    Optional<ResourceHandler> customHandler = handlers.stream()
        .filter(handler -> handler.isHandlerForResource(config, resource)).findAny();

    return customHandler.orElseGet(() -> getResourceHandler(resource));

  }

  @Override
  public ResourceHandler getResourceHandler(HasMetadata resource) {
    Instance<ResourceHandler> kindHandler = handlers
        .select(new KindLiteral(resource.getClass()));

    if (kindHandler.isResolvable()) {
      return kindHandler.get();
    }

    return handlers.select(DefaultResourceHandler.class).get();
  }

}
