/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgscript.ScriptEventReason;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.event.EventEmitterType;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ScriptReconciliator
    extends AbstractReconciliator<StackGresScript> {

  public ScriptReconciliator() {
    super(StackGresScript.KIND);
  }

  private EventEmitter<StackGresScript> eventController;

  private CustomResourceScheduler<StackGresScript> scriptScheduler;

  private ScriptStatusManager statusManager;

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

  @Inject
  public void setScriptScheduler(CustomResourceScheduler<StackGresScript> scriptScheduler) {
    this.scriptScheduler = scriptScheduler;
  }

  @Inject
  public void setEventController(
      @EventEmitterType(StackGresScript.class)
      EventEmitter<StackGresScript> eventController) {
    this.eventController = eventController;
  }

  @Inject
  public void setStatusManager(ScriptStatusManager statusManager) {
    this.statusManager = statusManager;
  }

}
