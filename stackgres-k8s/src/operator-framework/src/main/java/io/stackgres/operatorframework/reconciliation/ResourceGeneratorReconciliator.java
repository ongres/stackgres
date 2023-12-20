/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;

public abstract class ResourceGeneratorReconciliator<
      T extends ResourceHandlerContext,
      H extends CustomResource<?, ?>,
      S extends ResourceHandlerSelector<T>>
    extends Reconciliator<T> {

  protected final String name;
  protected final S handlerSelector;
  protected final ObjectMapper objectMapper;
  protected final Function<T, H> resourceGetter;

  protected ResourceGeneratorReconciliator(
      String name, Function<T, H> resourceGetter,
      S handlerSelector, ObjectMapper objectMapper) {
    super();
    this.name = name;
    this.resourceGetter = resourceGetter;
    this.handlerSelector = handlerSelector;
    this.objectMapper = objectMapper;
  }

  private enum Operation {
    NONE,
    CREATED,
    UPDATED
  }

  @Override
  public ReconciliationResult<?> reconcile(KubernetesClient client, T context) {
    onPreConfigReconcilied(client, context);

    H contextResource = resourceGetter.apply(context);
    deleteUnwantedResources(client, context);
    Operation result = createOrUpdateRequiredResources(client, context);

    if (result == Operation.UPDATED) {
      logger.info("{} updated: '{}.{}'", name,
          contextResource.getMetadata().getNamespace(),
          contextResource.getMetadata().getName());
      onConfigUpdated(client, context);
    }

    if (result == Operation.CREATED) {
      logger.info("{} created: '{}.{}'", name,
          contextResource.getMetadata().getNamespace(),
          contextResource.getMetadata().getName());
      onConfigCreated(client, context);
    }

    onPostConfigReconcilied(client, context);

    logger.debug("{} synced: '{}.{}'", name,
        contextResource.getMetadata().getNamespace(),
        contextResource.getMetadata().getName());

    return new ReconciliationResult<>();
  }

  private void deleteUnwantedResources(KubernetesClient client, T context) {
    for (Tuple2<HasMetadata, Optional<HasMetadata>> existingResource : context
        .getExistingResources()) {
      final ImmutableMap<String, String> labels = context.getLabels();
      if (!labels.entrySet().stream().allMatch(
          entry -> Objects.equals(entry.getValue(),
              existingResource.v1.getMetadata().getLabels().get(entry.getKey())))) {
        if (handlerSelector.skipDeletion(context, existingResource.v1)) {
          logger.trace("Skip deletion of resource {}.{} of type {}",
              existingResource.v1.getMetadata().getNamespace(),
              existingResource.v1.getMetadata().getName(),
              existingResource.v1.getKind());
          continue;
        }
        logger.debug("Deleting resource {}.{} of type {}"
            + " since do not belong to any existing {}",
            existingResource.v1.getMetadata().getNamespace(),
            existingResource.v1.getMetadata().getName(),
            existingResource.v1.getKind(), name);
        handlerSelector.delete(client, context, existingResource.v1);
      } else if (existingResource.v2.isEmpty()
          && !handlerSelector.isManaged(context, existingResource.v1)) {
        if (handlerSelector.skipDeletion(context, existingResource.v1)) {
          logger.trace("Skip deletion of resource {}.{} of type {}",
              existingResource.v1.getMetadata().getNamespace(),
              existingResource.v1.getMetadata().getName(),
              existingResource.v1.getKind());
          continue;
        }
        logger.debug("Deleting resource {}.{} of type {}"
            + " since do not belong to existing {}",
            existingResource.v1.getMetadata().getNamespace(),
            existingResource.v1.getMetadata().getName(),
            existingResource.v1.getKind(), name);
        handlerSelector.delete(client, context, existingResource.v1);
      }
    }
  }

  private Operation createOrUpdateRequiredResources(KubernetesClient client, T context) {
    boolean created = false;
    boolean updated = false;
    for (Tuple2<HasMetadata, Optional<HasMetadata>> requiredResource : context
        .getRequiredResources()) {
      Optional<HasMetadata> matchingResource = requiredResource.v2;
      if (matchingResource
          .map(existingResource -> handlerSelector.equals(
              context, existingResource, requiredResource.v1))
          .orElse(false)) {
        logger.trace("Found resource {}.{} of type {}",
            requiredResource.v1.getMetadata().getNamespace(),
            requiredResource.v1.getMetadata().getName(),
            requiredResource.v1.getKind());
        continue;
      }
      if (matchingResource.isPresent()) {
        HasMetadata existingResource = matchingResource.get();
        if (handlerSelector.skipUpdate(context, existingResource, requiredResource.v1)) {
          logger.trace("Skip update for resource {}.{} of type {}",
              requiredResource.v1.getMetadata().getNamespace(),
              requiredResource.v1.getMetadata().getName(),
              requiredResource.v1.getKind());
          continue;
        }
        logger.debug("Updating resource {}.{} of type {}"
            + " to meet {} requirements",
            existingResource.getMetadata().getNamespace(),
            existingResource.getMetadata().getName(),
            existingResource.getKind(), name);
        HasMetadata updatedExistingResource = Unchecked.supplier(() -> objectMapper.treeToValue(
            objectMapper.valueToTree(existingResource), existingResource.getClass())).get();
        handlerSelector.update(context, updatedExistingResource, requiredResource.v1);
        HasMetadata patchedResource = handlerSelector
            .patch(client, context, updatedExistingResource);
        if (!handlerSelector.equals(context, existingResource, patchedResource)) {
          updated = true;
        }
      } else {
        if (handlerSelector.skipCreation(context, requiredResource.v1)) {
          logger.trace("Skip creation for resource {}.{} of type {}",
              requiredResource.v1.getMetadata().getNamespace(),
              requiredResource.v1.getMetadata().getName(),
              requiredResource.v1.getKind());
          continue;
        }
        logger.debug("Creating resource {}.{} of type {}",
            requiredResource.v1.getMetadata().getNamespace(),
            requiredResource.v1.getMetadata().getName(),
            requiredResource.v1.getKind());
        HasMetadata updatedRequiredResource = Unchecked.supplier(() -> objectMapper.treeToValue(
            objectMapper.valueToTree(requiredResource.v1), requiredResource.v1.getClass())).get();
        handlerSelector.update(context, updatedRequiredResource, requiredResource.v1);
        HasMetadata createdResource = handlerSelector
            .create(client, context, updatedRequiredResource);
        if (createdResource != null) {
          created = true;
        }
      }
    }

    if (updated) {
      return Operation.UPDATED;
    }

    if (created) {
      return Operation.CREATED;
    }

    return Operation.NONE;
  }

  protected void onPreConfigReconcilied(KubernetesClient client, T context) {
  }

  protected void onConfigCreated(KubernetesClient client, T context) {
  }

  protected void onConfigUpdated(KubernetesClient client, T context) {
  }

  protected void onPostConfigReconcilied(KubernetesClient client, T context) {
  }

}
