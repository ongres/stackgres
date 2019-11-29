/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.ResourceHandlerSelector;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterReconciliator implements ResourceHandlerContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterReconciliator.class);

  private final ResourceHandlerSelector handlerSelector;
  private final ClusterStatusManager statusManager;
  private final EventController eventController;
  private final KubernetesClient client;
  private final ObjectMapper objectMapper;
  private final StackGresClusterConfig clusterConfig;
  private final StackGresCluster cluster;
  private final ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources;
  private final ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources;

  private ClusterReconciliator(Builder builder) {
    Objects.requireNonNull(builder.handlerSelector);
    Objects.requireNonNull(builder.statusManager);
    Objects.requireNonNull(builder.eventController);
    Objects.requireNonNull(builder.client);
    Objects.requireNonNull(builder.objectMapper);
    Objects.requireNonNull(builder.clusterConfig);
    Objects.requireNonNull(builder.existingResources);
    Objects.requireNonNull(builder.requiredResources);
    this.handlerSelector = builder.handlerSelector;
    this.statusManager = builder.statusManager;
    this.eventController = builder.eventController;
    this.client = builder.client;
    this.objectMapper = builder.objectMapper;
    this.clusterConfig = builder.clusterConfig;
    this.cluster = builder.clusterConfig.getCluster();
    this.existingResources = builder.existingResources;
    this.requiredResources = builder.requiredResources;
  }

  public StackGresClusterConfig getClusterConfig() {
    return clusterConfig;
  }

  public ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getExistingResources() {
    return existingResources;
  }

  public ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getRequiredResources() {
    return requiredResources;
  }

  void reconcile() {
    boolean created = false;
    boolean updated = false;
    for (Tuple2<HasMetadata, Optional<HasMetadata>> existingResource : existingResources) {
      if (existingResource.v1.getMetadata().getOwnerReferences().stream()
          .map(ownerReference -> ownerReference.getApiVersion()
              .equals(cluster.getApiVersion())
              && ownerReference.getKind()
              .equals(cluster.getKind())
              && ownerReference.getName()
              .equals(cluster.getMetadata().getName())
              && ownerReference.getUid()
              .equals(cluster.getMetadata().getUid()))
          .map(resourceBelongsToCurrentCluster -> !resourceBelongsToCurrentCluster)
          .findFirst()
          .orElse(true)
          && !handlerSelector.isManaged(clusterConfig, existingResource.v1)) {
        if (handlerSelector.skipDeletion(clusterConfig, existingResource.v1)) {
          LOGGER.trace("Skip deletion for resource {}.{} of type {}",
              existingResource.v1.getMetadata().getNamespace(),
              existingResource.v1.getMetadata().getName(),
              existingResource.v1.getKind());
          continue;
        }
        LOGGER.debug("Deleteing resource {}.{} of type {}"
            + " since belong to an older version of an existing cluster",
            existingResource.v1.getMetadata().getNamespace(),
            existingResource.v1.getMetadata().getName(),
            existingResource.v1.getKind());
        handlerSelector.delete(client, clusterConfig, existingResource.v1);
      } else if (!existingResource.v2.isPresent()
          && !handlerSelector.isManaged(clusterConfig, existingResource.v1)) {
        if (handlerSelector.skipDeletion(clusterConfig, existingResource.v1)) {
          LOGGER.trace("Skip deletion for resource {}.{} of type {}",
              existingResource.v1.getMetadata().getNamespace(),
              existingResource.v1.getMetadata().getName(),
              existingResource.v1.getKind());
          continue;
        }
        LOGGER.debug("Deleteing resource {}.{} of type {}"
            + " since does not belong to existing cluster",
            existingResource.v1.getMetadata().getNamespace(),
            existingResource.v1.getMetadata().getName(),
            existingResource.v1.getKind());
        handlerSelector.delete(client, clusterConfig, existingResource.v1);
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
            + " to meet cluster requirements",
            existingResource.getMetadata().getNamespace(),
            existingResource.getMetadata().getName(),
            existingResource.getKind());
        HasMetadata updatedExistingResource = Unchecked.supplier(() -> objectMapper.treeToValue(
            objectMapper.valueToTree(existingResource), existingResource.getClass())).get();
        handlerSelector.update(this, updatedExistingResource, requiredResource.v1);
        handlerSelector.patch(client, clusterConfig, updatedExistingResource);
        updated = true;
      } else {
        if (handlerSelector.skipCreation(clusterConfig, requiredResource.v1)) {
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
        handlerSelector.create(client, clusterConfig, requiredResource.v1);
        created = true;
      }
    }

    if (updated) {
      LOGGER.info("Cluster updated: '{}.{}'",
          cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName());
      eventController.sendEvent(EventReason.CLUSTER_UPDATED,
          "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
          + cluster.getMetadata().getName() + " updated", cluster);
      statusManager.updatePendingRestart(cluster);
      statusManager.sendCondition(ClusterStatusCondition.FALSE_FAILED, cluster);
    }

    if (created && !updated) {
      LOGGER.info("Cluster created: '{}.{}'",
          cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName());
      eventController.sendEvent(EventReason.CLUSTER_CREATED,
          "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
          + cluster.getMetadata().getName() + " created", cluster);
    }

    LOGGER.debug("Cluster synced: '{}.{}'",
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName());
  }

  /**
   * Creates builder to build {@link ClusterReconciliator}.
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link ClusterReconciliator}.
   */
  public static final class Builder {
    private ResourceHandlerSelector handlerSelector;
    private ClusterStatusManager statusManager;
    private EventController eventController;
    private KubernetesClient client;
    private ObjectMapper objectMapper;
    private StackGresClusterConfig clusterConfig;
    private ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources;
    private ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources;

    private Builder() {}

    public Builder withHandlerSelector(ResourceHandlerSelector handlerSelector) {
      this.handlerSelector = handlerSelector;
      return this;
    }

    public Builder withStatusManager(ClusterStatusManager statusManager) {
      this.statusManager = statusManager;
      return this;
    }

    public Builder withEventController(EventController eventController) {
      this.eventController = eventController;
      return this;
    }

    public Builder withClient(KubernetesClient client) {
      this.client = client;
      return this;
    }

    public Builder withObjectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    public Builder withClusterConfig(StackGresClusterConfig clusterConfig) {
      this.clusterConfig = clusterConfig;
      return this;
    }

    public Builder withExistingResources(
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
      this.existingResources = existingResources;
      return this;
    }

    public Builder withRequiredResources(
        ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources) {
      this.requiredResources = requiredResources;
      return this;
    }

    public ClusterReconciliator build() {
      return new ClusterReconciliator(this);
    }
  }

}
