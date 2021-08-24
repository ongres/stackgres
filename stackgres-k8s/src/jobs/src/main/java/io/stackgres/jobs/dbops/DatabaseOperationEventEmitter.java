/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

public interface DatabaseOperationEventEmitter {

  void operationStarted(String dbOpName, String namespace);

  void operationCompleted(String dbOpName, String namespace);

  void operationFailed(String dbOpName, String namespace);

  void operationTimedOut(String dbOpName, String namespace);
}
