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
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliator;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;

import org.jooq.lambda.tuple.Tuple2;

public class ClusterReconciliator extends AbstractReconciliator<StackGresClusterContext> {

  private final ClusterStatusManager statusManager;
  private final EventController eventController;

  private ClusterReconciliator(Builder builder) {
    super("Cluster", builder.handlerSelector,
        builder.client, builder.objectMapper,
        builder.clusterContext, builder.clusterContext.getCluster(),
        builder.requiredResources,
        builder.existingResources);
    Objects.requireNonNull(builder.handlerSelector);
    Objects.requireNonNull(builder.statusManager);
    Objects.requireNonNull(builder.eventController);
    Objects.requireNonNull(builder.client);
    Objects.requireNonNull(builder.objectMapper);
    Objects.requireNonNull(builder.clusterContext);
    Objects.requireNonNull(builder.existingResources);
    Objects.requireNonNull(builder.requiredResources);
    this.statusManager = builder.statusManager;
    this.eventController = builder.eventController;
  }

  @Override
  protected void onConfigCreated() {
    eventController.sendEvent(EventReason.CLUSTER_CREATED,
        "StackGres Cluster " + contextResource.getMetadata().getNamespace() + "."
        + contextResource.getMetadata().getName() + " created", contextResource);
  }

  @Override
  protected void onConfigUpdated() {
    eventController.sendEvent(EventReason.CLUSTER_UPDATED,
        "StackGres Cluster " + contextResource.getMetadata().getNamespace() + "."
        + contextResource.getMetadata().getName() + " updated", contextResource);
    statusManager.updatePendingRestart((StackGresCluster) contextResource);
    statusManager.sendCondition(ClusterStatusCondition.FALSE_FAILED,
        (StackGresCluster) contextResource);
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
    private ResourceHandlerSelector<StackGresClusterContext> handlerSelector;
    private ClusterStatusManager statusManager;
    private EventController eventController;
    private KubernetesClient client;
    private ObjectMapper objectMapper;
    private StackGresClusterContext clusterContext;
    private ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources;
    private ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources;

    private Builder() {}

    public Builder withHandlerSelector(
        ResourceHandlerSelector<StackGresClusterContext> handlerSelector) {
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

    public Builder withClusterContext(StackGresClusterContext clusterContext) {
      this.clusterContext = clusterContext;
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
