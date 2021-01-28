/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class AbstractResourceHandlerSelector<T extends ResourceHandlerContext>
    implements ResourceHandlerSelector<T> {

  @Override
  public boolean equals(T resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return selectResourceHandler(resourceHandlerContext, requiredResource)
        .equals(resourceHandlerContext, existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(T resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return selectResourceHandler(resourceHandlerContext, requiredResource)
        .update(resourceHandlerContext, existingResource, requiredResource);
  }

  @Override
  public boolean isManaged(T context, HasMetadata existingResource) {
    return selectResourceHandler(context, existingResource).isManaged();
  }

  @Override
  public boolean skipCreation(T context, HasMetadata requiredResource) {
    return selectResourceHandler(context, requiredResource).skipCreation(context, requiredResource);
  }

  @Override
  public boolean skipUpdate(T context, HasMetadata existingResource, HasMetadata requiredResource) {
    return selectResourceHandler(context, requiredResource)
        .skipUpdate(context, existingResource, requiredResource);
  }

  @Override
  public boolean skipDeletion(T context, HasMetadata existingResource) {
    return selectResourceHandler(context, existingResource).skipDeletion(context, existingResource);
  }

  @Override
  public void registerKinds() {
    getResourceHandlers()
        .forEach(ResourceHandler::registerKind);
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client, T context) {
    return getResourceHandlers()
        .flatMap(handler -> handler.getResources(client, context));
  }

  @Override
  public Optional<HasMetadata> find(KubernetesClient client, T context, HasMetadata resource) {
    return selectResourceHandler(context, resource).find(client, resource);
  }

  @Override
  public HasMetadata create(KubernetesClient client, T context, HasMetadata resource) {
    return selectResourceHandler(context, resource).create(client, resource);
  }

  @Override
  public HasMetadata patch(KubernetesClient client, T context, HasMetadata resource) {
    return selectResourceHandler(context, resource).patch(client, resource);
  }

  @Override
  public boolean delete(KubernetesClient client, T context, HasMetadata resource) {
    return selectResourceHandler(context, resource).delete(client, resource);
  }

  private ResourceHandler<T> selectResourceHandler(T context, HasMetadata resource) {
    Optional<ResourceHandler<T>> customHandler = getResourceHandlers()
        .filter(handler -> handler.isHandlerForResource(context, resource)).findAny();

    return customHandler.orElseGet(() -> getResourceHandler(resource));

  }

  @Override
  public ResourceHandler<T> getResourceHandler(HasMetadata resource) {
    Optional<ResourceHandler<T>> customHandler = getResourceHandlers()
        .filter(handler -> handler.isHandlerForResource(resource)).findAny();

    if (customHandler.isPresent()) {
      return customHandler.get();
    }

    return getDefaultResourceHandler()
        .orElseThrow(() -> new IllegalStateException("Can not find handler for resource "
            + resource.getMetadata().getNamespace() + "." + resource.getMetadata().getName()
            + " of kind " + resource.getKind()));
  }

  protected abstract Stream<ResourceHandler<T>> getResourceHandlers();

  protected abstract Optional<ResourceHandler<T>> getDefaultResourceHandler();

}
