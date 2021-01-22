/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operatorframework.resource.EventReason;

@ApplicationScoped
public class DistributedLogsEventEmitter extends AbstractEventEmitter<StackGresDistributedLogs> {

  @Override
  public void sendEvent(EventReason reason, String message, StackGresDistributedLogs context) {

    emitEvent(reason, message, context);

  }

}
