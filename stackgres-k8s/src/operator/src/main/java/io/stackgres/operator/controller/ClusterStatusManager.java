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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterContext;

@ApplicationScoped
public class ClusterStatusManager extends AbstractClusterStatusManager<
    StackGresClusterContext, StackGresClusterCondition> {

  @Inject
  public ClusterStatusManager(KubernetesClientFactory clientFactory,
      LabelFactory<StackGresCluster> labelFactory) {
    super(clientFactory, labelFactory);
  }

  public ClusterStatusManager() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected List<StackGresClusterCondition> getConditions(
      StackGresClusterContext context) {
    return Optional.ofNullable(context.getCluster().getStatus())
        .map(StackGresClusterStatus::getConditions)
        .orElseGet(() -> new ArrayList<>());
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
  protected void patch(StackGresClusterContext context,
      KubernetesClient client) {
    StackGresCluster cluster = context.getCluster();
    client.customResources(StackGresClusterDefinition.CONTEXT,
        StackGresCluster.class,
        StackGresClusterList.class,
        StackGresClusterDoneable.class)
        .inNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getMetadata().getName())
        .patch(cluster);
  }

  @Override
  protected StackGresClusterCondition getFalsePendingRestart() {
    return ClusterStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  @Override
  protected StackGresClusterCondition getPodRequiresRestart() {
    return ClusterStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }

}
