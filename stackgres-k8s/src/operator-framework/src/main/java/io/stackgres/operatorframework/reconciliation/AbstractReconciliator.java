/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliator<T> implements ResourceHandlerContext<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractReconciliator.class);

  protected final String name;
  protected final ResourceHandlerSelector<T> handlerSelector;
  protected final KubernetesClient client;
  protected final ObjectMapper objectMapper;
  protected final T context;
  protected final HasMetadata contextResource;
  protected final ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources;
  protected final ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources;

  protected AbstractReconciliator(String name, ResourceHandlerSelector<T> handlerSelector,
      KubernetesClient client, ObjectMapper objectMapper, T context, HasMetadata contextResource,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    super();
    this.name = name;
    this.handlerSelector = handlerSelector;
    this.client = client;
    this.objectMapper = objectMapper;
    this.context = context;
    this.contextResource = contextResource;
    this.requiredResources = requiredResources;
    this.existingResources = existingResources;
  }

  @Override
  public T getConfig() {
    return context;
  }

  @Override
  public ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getExistingResources() {
    return existingResources;
  }

  @Override
  public ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getRequiredResources() {
    return requiredResources;
  }

  private enum Operation {
    NONE,
    CREATED,
    UPDATED
  }

  void reconcile() {
    deleteUnwantedResources();
    Operation result = createOrUpdateRequiredResources();

    if (result == Operation.UPDATED) {
      LOGGER.info(name + " updated: '{}.{}'",
          contextResource.getMetadata().getNamespace(),
          contextResource.getMetadata().getName());
      onConfigUpdated();
    }

    if (result == Operation.CREATED) {
      LOGGER.info(name + " created: '{}.{}'",
          contextResource.getMetadata().getNamespace(),
          contextResource.getMetadata().getName());
      onConfigCreated();
    }

    LOGGER.debug(name + " synced: '{}.{}'",
        contextResource.getMetadata().getNamespace(),
        contextResource.getMetadata().getName());
  }

  private void deleteUnwantedResources() {
    for (Tuple2<HasMetadata, Optional<HasMetadata>> existingResource : existingResources) {
      if (existingResource.v1.getMetadata().getOwnerReferences().stream()
          .map(ownerReference -> ownerReference.getApiVersion()
              .equals(contextResource.getApiVersion())
              && ownerReference.getKind()
              .equals(contextResource.getKind())
              && ownerReference.getName()
              .equals(contextResource.getMetadata().getName())
              && ownerReference.getUid()
              .equals(contextResource.getMetadata().getUid()))
          .map(resourceBelongsToCurrentConfig -> !resourceBelongsToCurrentConfig)
          .findFirst()
          .orElse(true)
          && !handlerSelector.isManaged(context, existingResource.v1)) {
        if (handlerSelector.skipDeletion(context, existingResource.v1)) {
          LOGGER.trace("Skip deletion for resource {}.{} of type {}",
              existingResource.v1.getMetadata().getNamespace(),
              existingResource.v1.getMetadata().getName(),
              existingResource.v1.getKind());
          continue;
        }
        LOGGER.debug("Deleteing resource {}.{} of type {}"
            + " since belong to an older version of an existing " + name,
            existingResource.v1.getMetadata().getNamespace(),
            existingResource.v1.getMetadata().getName(),
            existingResource.v1.getKind());
        handlerSelector.delete(client, context, existingResource.v1);
      } else if (!existingResource.v2.isPresent()
          && !handlerSelector.isManaged(context, existingResource.v1)) {
        if (handlerSelector.skipDeletion(context, existingResource.v1)) {
          LOGGER.trace("Skip deletion for resource {}.{} of type {}",
              existingResource.v1.getMetadata().getNamespace(),
              existingResource.v1.getMetadata().getName(),
              existingResource.v1.getKind());
          continue;
        }
        LOGGER.debug("Deleteing resource {}.{} of type {}"
            + " since does not belong to existing " + name,
            existingResource.v1.getMetadata().getNamespace(),
            existingResource.v1.getMetadata().getName(),
            existingResource.v1.getKind());
        handlerSelector.delete(client, context, existingResource.v1);
      }
    }
  }

  private Operation createOrUpdateRequiredResources() {
    boolean created = false;
    boolean updated = false;
    for (Tuple2<HasMetadata, Optional<HasMetadata>> requiredResource : requiredResources) {
      Optional<HasMetadata> matchingResource = requiredResource.v2;
      if (matchingResource
          .map(existingResource -> handlerSelector.equals(
              this, existingResource, requiredResource.v1))
          .orElse(false)) {
        LOGGER.trace("Found resource {}.{} of type {}",
            requiredResource.v1.getMetadata().getNamespace(),
            requiredResource.v1.getMetadata().getName(),
            requiredResource.v1.getKind());
        continue;
      }
      if (matchingResource.isPresent()) {
        HasMetadata existingResource = matchingResource.get();
        LOGGER.debug("Updating resource {}.{} of type {}"
            + " to meet " + name + " requirements",
            existingResource.getMetadata().getNamespace(),
            existingResource.getMetadata().getName(),
            existingResource.getKind());
        HasMetadata updatedExistingResource = Unchecked.supplier(() -> objectMapper.treeToValue(
            objectMapper.valueToTree(existingResource), existingResource.getClass())).get();
        handlerSelector.update(this, updatedExistingResource, requiredResource.v1);
        handlerSelector.patch(client, context, updatedExistingResource);
        updated = true;
      } else {
        if (handlerSelector.skipCreation(context, requiredResource.v1)) {
          LOGGER.trace("Skip creation for resource {}.{} of type {}",
              requiredResource.v1.getMetadata().getNamespace(),
              requiredResource.v1.getMetadata().getName(),
              requiredResource.v1.getKind());
          continue;
        }
        LOGGER.debug("Creating resource {}.{} of type {}",
            requiredResource.v1.getMetadata().getNamespace(),
            requiredResource.v1.getMetadata().getName(),
            requiredResource.v1.getKind());
        HasMetadata updatedRequiredResource = Unchecked.supplier(() -> objectMapper.treeToValue(
            objectMapper.valueToTree(requiredResource.v1), requiredResource.v1.getClass())).get();
        handlerSelector.update(this, updatedRequiredResource, requiredResource.v1);
        handlerSelector.create(client, context, updatedRequiredResource);
        created = true;
      }
    }

    if (updated) {
      return Operation.UPDATED;
    }

    if (created && ! updated) {
      return Operation.CREATED;
    }

    return Operation.NONE;
  }

  protected abstract void onConfigCreated();

  protected abstract void onConfigUpdated();

}
