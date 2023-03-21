/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ConditionUpdater;

@ApplicationScoped
public class DistributedLogsStatusManager
    extends ConditionUpdater<StackGresDistributedLogsContext, Condition> {

  @Override
  protected List<Condition> getConditions(
      StackGresDistributedLogsContext context) {
    return Optional.ofNullable(context.getDistributedLogs().getStatus())
        .map(StackGresDistributedLogsStatus::getConditions)
        .orElseGet(ArrayList::new);
  }

  @Override
  protected void setConditions(StackGresDistributedLogsContext context,
      List<Condition> conditions) {
    if (context.getDistributedLogs().getStatus() == null) {
      context.getDistributedLogs().setStatus(new StackGresDistributedLogsStatus());
    }
    context.getDistributedLogs().getStatus().setConditions(conditions);
  }
}
