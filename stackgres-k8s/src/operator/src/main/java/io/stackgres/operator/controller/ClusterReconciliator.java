/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.ClusterResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliator;

public class ClusterReconciliator
    extends AbstractReconciliator<StackGresClusterContext, StackGresCluster,
      ClusterResourceHandlerSelector> {

  private final ClusterStatusManager statusManager;
  private final EventController eventController;

  private ClusterReconciliator(Builder builder) {
    super("Cluster", builder.handlerSelector,
        builder.client, builder.objectMapper,
        builder.clusterContext, builder.clusterContext.getCluster());
    Objects.requireNonNull(builder.handlerSelector);
    Objects.requireNonNull(builder.statusManager);
    Objects.requireNonNull(builder.eventController);
    Objects.requireNonNull(builder.client);
    Objects.requireNonNull(builder.objectMapper);
    Objects.requireNonNull(builder.clusterContext);
    this.statusManager = builder.statusManager;
    this.eventController = builder.eventController;
  }

  @Override
  protected void onConfigCreated() {
    eventController.sendEvent(EventReason.CLUSTER_CREATED,
        "StackGres Cluster " + contextResource.getMetadata().getNamespace() + "."
        + contextResource.getMetadata().getName() + " created", contextResource);
    statusManager.sendCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), context);
  }

  @Override
  protected void onConfigUpdated() {
    eventController.sendEvent(EventReason.CLUSTER_UPDATED,
        "StackGres Cluster " + contextResource.getMetadata().getNamespace() + "."
        + contextResource.getMetadata().getName() + " updated", contextResource);
    statusManager.sendCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), context);
  }

  @Override
  protected void onPostConfigReconcilied() {
    statusManager.updatePendingRestart(context);
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
    private ClusterResourceHandlerSelector handlerSelector;
    private ClusterStatusManager statusManager;
    private EventController eventController;
    private KubernetesClient client;
    private ObjectMapper objectMapper;
    private StackGresClusterContext clusterContext;

    private Builder() {}

    public Builder withHandlerSelector(
        ClusterResourceHandlerSelector handlerSelector) {
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

    public ClusterReconciliator build() {
      return new ClusterReconciliator(this);
    }
  }

}
