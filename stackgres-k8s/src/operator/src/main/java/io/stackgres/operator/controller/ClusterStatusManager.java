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
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterContext;

@ApplicationScoped
public class ClusterStatusManager
    extends AbstractClusterStatusManager<StackGresClusterContext, StackGresClusterCondition> {

  @Inject
  public ClusterStatusManager(LabelFactory<StackGresCluster> labelFactory) {
    super(labelFactory);
  }

  public ClusterStatusManager() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected List<StackGresClusterCondition> getConditions(
      StackGresClusterContext context) {
    return Optional.ofNullable(context.getCluster().getStatus())
        .map(StackGresClusterStatus::getConditions)
        .orElseGet(ArrayList::new);
  }

  @Override
  protected void setConditions(StackGresClusterContext context,
      List<StackGresClusterCondition> conditions) {
    if (context.getCluster().getStatus() == null) {
      context.getCluster().setStatus(new StackGresClusterStatus());
    }
    context.getCluster().getStatus().setConditions(conditions);
  }

  @Override
  protected StackGresClusterCondition getFalsePendingRestart() {
    return ClusterStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  @Override
  protected StackGresClusterCondition getPodRequiresRestart() {
    return ClusterStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }

  @Override
  protected Optional<List<StackGresClusterPodStatus>> getPodStatuses(
      StackGresClusterContext context) {
    return Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses);
  }

}
