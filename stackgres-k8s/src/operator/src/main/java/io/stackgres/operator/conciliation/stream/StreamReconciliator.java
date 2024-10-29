/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StreamEventReason;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class StreamReconciliator
    extends AbstractReconciliator<StackGresStream> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresStream> scanner;
    @Inject CustomResourceFinder<StackGresStream> finder;
    @Inject AbstractConciliator<StackGresStream> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresStream> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject EventEmitter<StackGresStream> eventController;
    @Inject StreamStatusManager statusManager;
    @Inject CustomResourceScheduler<StackGresStream> streamScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
  }

  private final EventEmitter<StackGresStream> eventController;
  private final PatchResumer<StackGresStream> patchResumer;
  private final StreamStatusManager statusManager;
  private final CustomResourceScheduler<StackGresStream> streamScheduler;

  @Inject
  public StreamReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        StackGresStream.KIND);
    this.eventController = parameters.eventController;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
    this.statusManager = parameters.statusManager;
    this.streamScheduler = parameters.streamScheduler;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresStream configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected void onPreReconciliation(StackGresStream config) {
  }

  @Override
  protected void onPostReconciliation(StackGresStream config) {
    streamScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onConfigCreated(StackGresStream stream, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(stream, result);
    eventController.sendEvent(StreamEventReason.STREAM_CREATED,
        "Stream " + stream.getMetadata().getNamespace() + "."
            + stream.getMetadata().getName() + " created: " + resourceChanged, stream);
  }

  @Override
  protected void onConfigUpdated(StackGresStream stream, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(stream, result);
    eventController.sendEvent(StreamEventReason.STREAM_UPDATED,
        "Stream " + stream.getMetadata().getNamespace() + "."
            + stream.getMetadata().getName() + " updated: " + resourceChanged, stream);
  }

  @Override
  protected void onError(Exception ex, StackGresStream stream) {
    String message = MessageFormatter.arrayFormat(
        "Stream reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(StreamEventReason.STREAM_CONFIG_ERROR,
        message + ": " + ex.getMessage(), stream);
  }

}
