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
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ScriptReconciliator
    extends AbstractReconciliator<StackGresScript> {

  @Dependent
  public static class Parameters {
    @Inject CustomResourceScanner<StackGresScript> scanner;
    @Inject Conciliator<StackGresScript> conciliator;
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
    super(parameters.scanner, parameters.conciliator, parameters.handlerDelegator,
        parameters.client, StackGresScript.KIND);
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
  public void onPreReconciliation(StackGresScript config) {
    scriptScheduler.update(config,
        (targetScript, script) -> {
          statusManager.refreshCondition(targetScript);
        });
  }

  @Override
  public void onPostReconciliation(StackGresScript config) {
    // Nothing to do
  }

  @Override
  public void onConfigCreated(StackGresScript script, ReconciliationResult result) {
    // Nothing to do
  }

  @Override
  public void onConfigUpdated(StackGresScript script, ReconciliationResult result) {
    // Nothing to do
  }

  @Override
  public void onError(Exception ex, StackGresScript script) {
    String message = MessageFormatter.arrayFormat(
        "Script reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ScriptEventReason.SCRIPT_CONFIG_ERROR,
        message + ": " + ex.getMessage(), script);
  }

}
