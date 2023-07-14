/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import java.util.function.Supplier;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatabaseOperationEventEmitterImpl implements DatabaseOperationEventEmitter {

  private final EventEmitter<StackGresDbOps> eventEmitter;
  private final CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Inject
  public DatabaseOperationEventEmitterImpl(
      EventEmitter<StackGresDbOps> eventEmitter,
      CustomResourceFinder<StackGresDbOps> dbOpsFinder) {
    this.eventEmitter = eventEmitter;
    this.dbOpsFinder = dbOpsFinder;
  }

  @Override
  public void operationStarted(String dbOpName, String namespace) {

    var dbOp = dbOpsFinder.findByNameAndNamespace(dbOpName, namespace)
        .orElseThrow(dbOpsNotFound(dbOpName, namespace));

    var operation = dbOp.getSpec().getOp();

    eventEmitter.sendEvent(DbOpsEvents.DB_OP_STARTED,
        "Database operation " + operation + " started", dbOp);
  }

  @Override
  public void operationCompleted(String dbOpName, String namespace) {

    var dbOp = dbOpsFinder.findByNameAndNamespace(dbOpName, namespace)
        .orElseThrow(dbOpsNotFound(dbOpName, namespace));

    var operation = dbOp.getSpec().getOp();

    eventEmitter.sendEvent(DbOpsEvents.DB_OP_COMPLETED,
        "Database operation " + operation + " completed", dbOp);

  }

  @Override
  public void operationFailed(String dbOpName, String namespace) {
    var dbOp = dbOpsFinder.findByNameAndNamespace(dbOpName, namespace)
        .orElseThrow(dbOpsNotFound(dbOpName, namespace));

    var operation = dbOp.getSpec().getOp();

    eventEmitter.sendEvent(DbOpsEvents.DB_OP_FAILED,
        "Database operation " + operation + " failed", dbOp);
  }

  @Override
  public void operationTimedOut(String dbOpName, String namespace) {

    var dbOp = dbOpsFinder.findByNameAndNamespace(dbOpName, namespace)
        .orElseThrow(dbOpsNotFound(dbOpName, namespace));

    var operation = dbOp.getSpec().getOp();

    eventEmitter.sendEvent(DbOpsEvents.DB_OP_TIMEOUT,
        "Database operation " + operation + " timed out", dbOp);

  }

  private Supplier<RuntimeException> dbOpsNotFound(String dbOpName, String namespace) {
    return () ->
        new IllegalArgumentException("DbOps " + dbOpName + "not found in namespace " + namespace);
  }

}
