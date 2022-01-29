/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;

@ApplicationScoped
public class DistributedLogsScheduler
    extends
    AbstractCustomResourceScheduler<StackGresDistributedLogs, StackGresDistributedLogsList> {

  public DistributedLogsScheduler() {
    super(StackGresDistributedLogs.class, StackGresDistributedLogsList.class);
  }

}
