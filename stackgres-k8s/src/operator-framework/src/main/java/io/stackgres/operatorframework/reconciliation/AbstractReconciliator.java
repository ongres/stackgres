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
  protected final T config;
  protected final HasMetadata configResource;
  protected final ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources;
  protected final ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources;

  protected AbstractReconciliator(String name, ResourceHandlerSelector<T> handlerSelector,
      KubernetesClient client, ObjectMapper objectMapper, T config, HasMetadata configResource,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    super();
    this.name = name;
    this.handlerSelector = handlerSelector;
    this.client = client;
    this.objectMapper = objectMapper;
    this.config = config;
    this.configResource = configResource;
    this.requiredResources = requiredResources;
    this.existingResources = existingResources;
  }

  @Override
  public T getConfig() {
    return config;
  }

  @Override
  public ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getExistingResources() {
    return existingResources;
  }

  @Override
  public ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getRequiredResources() {
    return requiredResources;
  }

  void reconcile() {
    boolean created = false;
    boolean updated = false;
    for (Tuple2<HasMetadata, Optional<HasMetadata>> existingResource : existingResources) {
      if (existingResource.v1.getMetadata().getOwnerReferences().stream()
          .map(ownerReference -> ownerReference.getApiVersion()
              .equals(configResource.getApiVersion())
              && ownerReference.getKind()
              .equals(configResource.getKind())
              && ownerReference.getName()
              .equals(configResource.getMetadata().getName())
              && ownerReference.getUid()
              .equals(configResource.getMetadata().getUid()))
          .map(resourceBelongsToCurrentConfig -> !resourceBelongsToCurrentConfig)
          .findFirst()
          .orElse(true)
          && !handlerSelector.isManaged(config, existingResource.v1)) {
        if (handlerSelector.skipDeletion(config, existingResource.v1)) {
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
        handlerSelector.delete(client, config, existingResource.v1);
      } else if (!existingResource.v2.isPresent()
          && !handlerSelector.isManaged(config, existingResource.v1)) {
        if (handlerSelector.skipDeletion(config, existingResource.v1)) {
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
        handlerSelector.delete(client, config, existingResource.v1);
      }
    }
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
        handlerSelector.patch(client, config, updatedExistingResource);
        updated = true;
      } else {
        if (handlerSelector.skipCreation(config, requiredResource.v1)) {
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
        handlerSelector.create(client, config, requiredResource.v1);
        created = true;
      }
    }

    if (updated) {
      LOGGER.info(name + " updated: '{}.{}'",
          configResource.getMetadata().getNamespace(),
          configResource.getMetadata().getName());
      onConfigUpdated();
    }

    if (created && !updated) {
      LOGGER.info(name + " created: '{}.{}'",
          configResource.getMetadata().getNamespace(),
          configResource.getMetadata().getName());
      onConfigCreated();
    }

    LOGGER.debug(name + " synced: '{}.{}'",
        configResource.getMetadata().getNamespace(),
        configResource.getMetadata().getName());
  }

  protected abstract void onConfigCreated();

  protected abstract void onConfigUpdated();

}
