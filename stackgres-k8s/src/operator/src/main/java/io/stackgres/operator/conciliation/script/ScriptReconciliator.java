/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgscript.ScriptEventReason;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ScriptReconciliator
    extends AbstractReconciliator<StackGresScript> {

  @Dependent
  public static class Parameters {
    @Inject CustomResourceScanner<StackGresScript> scanner;
    @Inject CustomResourceFinder<StackGresScript> finder;
    @Inject AbstractConciliator<StackGresScript> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresScript> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject EventEmitter<StackGresScript> eventController;
    @Inject CustomResourceScheduler<StackGresScript> scriptScheduler;
    @Inject ScriptStatusManager statusManager;
  }

  private final EventEmitter<StackGresScript> eventController;
  private final CustomResourceScheduler<StackGresScript> scriptScheduler;
  private final ScriptStatusManager statusManager;

  @Inject
  public ScriptReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        StackGresScript.KIND);
    this.eventController = parameters.eventController;
    this.scriptScheduler = parameters.scriptScheduler;
    this.statusManager = parameters.statusManager;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresScript configKey, boolean load) {
    super.reconciliationCycle(configKey, load);
  }

  @Override
  protected void onPreReconciliation(StackGresScript config) {
    scriptScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onPostReconciliation(StackGresScript config) {
    // Nothing to do
  }

  @Override
  protected void onConfigCreated(StackGresScript script, ReconciliationResult result) {
    // Nothing to do
  }

  @Override
  protected void onConfigUpdated(StackGresScript script, ReconciliationResult result) {
    // Nothing to do
  }

  @Override
  protected void onError(Exception ex, StackGresScript script) {
    String message = MessageFormatter.arrayFormat(
        "Script reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ScriptEventReason.SCRIPT_CONFIG_ERROR,
        message + ": " + ex.getMessage(), script);
  }

}
