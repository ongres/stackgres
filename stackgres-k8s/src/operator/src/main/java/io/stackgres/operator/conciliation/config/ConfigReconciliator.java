/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgconfig.ConfigEventReason;
import io.stackgres.common.crd.sgconfig.ConfigStatusCondition;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.OperatorLockHolder;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ConfigReconciliator
    extends AbstractReconciliator<StackGresConfig> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresConfig> scanner;
    @Inject CustomResourceFinder<StackGresConfig> finder;
    @Inject AbstractConciliator<StackGresConfig> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresConfig> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject StatusManager<StackGresConfig, Condition> statusManager;
    @Inject EventEmitter<StackGresConfig> eventController;
    @Inject CustomResourceScheduler<StackGresConfig> configScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
  }

  private final StatusManager<StackGresConfig, Condition> statusManager;
  private final EventEmitter<StackGresConfig> eventController;
  private final CustomResourceScheduler<StackGresConfig> configScheduler;
  private final PatchResumer<StackGresConfig> patchResumer;

  @Inject
  public ConfigReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        StackGresConfig.KIND);
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.configScheduler = parameters.configScheduler;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresConfig configKey, boolean load) {
    super.reconciliationCycle(configKey, load);
  }

  @Override
  protected void onPreReconciliation(StackGresConfig config) {
  }

  @Override
  protected void onPostReconciliation(StackGresConfig config) {
    statusManager.refreshCondition(config);

    configScheduler.updateStatus(config,
        (currentConfig) -> {
          if (config.getStatus() != null) {
            currentConfig.setStatus(config.getStatus());
          }
        });
  }

  @Override
  protected void onConfigCreated(StackGresConfig config, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(config, result);
    eventController.sendEvent(ConfigEventReason.CONFIG_CREATED,
        "Config " + config.getMetadata().getNamespace() + "."
            + config.getMetadata().getName() + " created: " + resourceChanged, config);
    statusManager.updateCondition(
        ConfigStatusCondition.FALSE_FAILED.getCondition(), config);
  }

  @Override
  protected void onConfigUpdated(StackGresConfig config, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(config, result);
    eventController.sendEvent(ConfigEventReason.CONFIG_UPDATED,
        "Config " + config.getMetadata().getNamespace() + "."
            + config.getMetadata().getName() + " updated: " + resourceChanged, config);
    statusManager.updateCondition(
        ConfigStatusCondition.FALSE_FAILED.getCondition(), config);
  }

  @Override
  protected void onError(Exception ex, StackGresConfig config) {
    String message = MessageFormatter.arrayFormat(
        "Config reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ConfigEventReason.CONFIG_ERROR,
        message + ": " + ex.getMessage(), config);
  }

}
