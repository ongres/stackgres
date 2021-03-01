/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ConditionUpdater;

@ApplicationScoped
public class DistributedLogsStatusManager
    extends ConditionUpdater<StackGresDistributedLogsContext, StackGresDistributedLogsCondition> {

  @Override
  protected List<StackGresDistributedLogsCondition> getConditions(
      StackGresDistributedLogsContext context) {
    return Optional.ofNullable(context.getDistributedLogs().getStatus())
        .map(StackGresDistributedLogsStatus::getConditions)
        .orElseGet(ArrayList::new);
  }

  @Override
  protected void setConditions(StackGresDistributedLogsContext context,
      List<StackGresDistributedLogsCondition> conditions) {
    if (context.getDistributedLogs().getStatus() == null) {
      context.getDistributedLogs().setStatus(new StackGresDistributedLogsStatus());
    }
    context.getDistributedLogs().getStatus().setConditions(conditions);
  }

  @Override
  protected void patch(StackGresDistributedLogsContext context,
      KubernetesClient client) {
    StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    client.customResources(StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class)
        .inNamespace(distributedLogs.getMetadata().getNamespace())
        .withName(distributedLogs.getMetadata().getName())
        .patch(distributedLogs);
  }

}
