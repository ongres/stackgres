/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operatorframework.resource.EventReason;

@ApplicationScoped
public class BackupEventEmitter extends AbstractEventEmitter<StackGresBackup> {

  @Override
  public void sendEvent(EventReason reason, String message, StackGresBackup involvedObject) {

    emitEvent(reason, message, involvedObject);
  }
}
