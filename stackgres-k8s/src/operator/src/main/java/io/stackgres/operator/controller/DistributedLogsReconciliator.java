/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operator.resource.DistributedLogsResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliator;

public class DistributedLogsReconciliator
    extends AbstractReconciliator<StackGresDistributedLogsContext,
      StackGresDistributedLogs, DistributedLogsResourceHandlerSelector> {

  private final DistributedLogsStatusManager statusManager;
  private final EventController eventController;

  private DistributedLogsReconciliator(Builder builder) {
    super("Centralized Logging", builder.handlerSelector,
        builder.client, builder.objectMapper,
        builder.distributedLogsContext,
        builder.distributedLogsContext.getDistributedLogs());
    Objects.requireNonNull(builder.handlerSelector);
    Objects.requireNonNull(builder.statusManager);
    Objects.requireNonNull(builder.eventController);
    Objects.requireNonNull(builder.client);
    Objects.requireNonNull(builder.objectMapper);
    Objects.requireNonNull(builder.distributedLogsContext);
    this.statusManager = builder.statusManager;
    this.eventController = builder.eventController;
  }

  @Override
  protected void onConfigCreated() {
    eventController.sendEvent(EventReason.DISTRIBUTED_LOGS_CREATED,
        "StackGres Centralized Logging " + contextResource.getMetadata().getNamespace() + "."
        + contextResource.getMetadata().getName() + " created", contextResource);
    statusManager.sendCondition(DistributedLogsStatusCondition.FALSE_FAILED, context);
  }

  @Override
  protected void onConfigUpdated() {
    eventController.sendEvent(EventReason.DISTRIBUTED_LOGS_UPDATED,
        "StackGres Centralized Logging " + contextResource.getMetadata().getNamespace() + "."
        + contextResource.getMetadata().getName() + " updated", contextResource);
    statusManager.updatePendingRestart(context);
    statusManager.sendCondition(DistributedLogsStatusCondition.FALSE_FAILED, context);
  }

  /**
   * Creates builder to build {@link DistributedLogsReconciliator}.
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link DistributedLogsReconciliator}.
   */
  public static final class Builder {
    private DistributedLogsResourceHandlerSelector handlerSelector;
    private DistributedLogsStatusManager statusManager;
    private EventController eventController;
    private KubernetesClient client;
    private ObjectMapper objectMapper;
    private StackGresDistributedLogsContext distributedLogsContext;

    private Builder() {}

    public Builder withHandlerSelector(
        DistributedLogsResourceHandlerSelector handlerSelector) {
      this.handlerSelector = handlerSelector;
      return this;
    }

    public Builder withStatusManager(DistributedLogsStatusManager statusManager) {
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

    public Builder withDistributedLogsContext(
        StackGresDistributedLogsContext distributedLogsContext) {
      this.distributedLogsContext = distributedLogsContext;
      return this;
    }

    public DistributedLogsReconciliator build() {
      return new DistributedLogsReconciliator(this);
    }
  }

}
