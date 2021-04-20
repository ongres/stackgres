/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.operator.common.StackGresDistributedLogsContext;

@ApplicationScoped
public class DistributedLogsStatusManager extends
    AbstractClusterStatusManager
    <StackGresDistributedLogsContext, StackGresDistributedLogsCondition> {

  @Inject
  public DistributedLogsStatusManager(LabelFactory<StackGresDistributedLogs> labelFactory) {
    super(labelFactory);
  }

  public DistributedLogsStatusManager() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

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
  protected StackGresDistributedLogsCondition getFalsePendingRestart() {
    return DistributedLogsStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  @Override
  protected StackGresDistributedLogsCondition getPodRequiresRestart() {
    return DistributedLogsStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }

  @Override
  protected Optional<List<StackGresClusterPodStatus>> getPodStatuses(
      StackGresDistributedLogsContext context) {
    return Optional.of(context.getDistributedLogs())
        .map(StackGresDistributedLogs::getStatus)
        .map(StackGresDistributedLogsStatus::getPodStatuses);
  }

}
