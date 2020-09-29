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
import io.stackgres.common.ArcUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDoneable;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class DistributedLogsStatusManager extends AbstractClusterStatusManager<
    StackGresDistributedLogsContext, StackGresDistributedLogsCondition> {

  @Inject
  public DistributedLogsStatusManager(KubernetesClientFactory clientFactory,
      LabelFactory<StackGresDistributedLogs> labelFactory) {
    super(clientFactory, labelFactory);
  }

  public DistributedLogsStatusManager() {
    super(null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  protected List<StackGresDistributedLogsCondition> getConditions(
      StackGresDistributedLogsContext context) {
    return Optional.ofNullable(context.getDistributedLogs().getStatus())
        .map(StackGresDistributedLogsStatus::getConditions)
        .orElseGet(() -> new ArrayList<>());
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
  protected void patchCluster(StackGresDistributedLogsContext context,
      KubernetesClient client) {
    StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    ResourceUtil.getCustomResource(client, StackGresDistributedLogsDefinition.NAME)
        .ifPresent(crd -> client.customResources(crd,
            StackGresDistributedLogs.class,
            StackGresDistributedLogsList.class,
            StackGresDistributedLogsDoneable.class)
            .inNamespace(distributedLogs.getMetadata().getNamespace())
            .withName(distributedLogs.getMetadata().getName())
            .patch(distributedLogs));
  }

  @Override
  protected StackGresDistributedLogsCondition getFalsePendingRestart() {
    return DistributedLogsStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  @Override
  protected StackGresDistributedLogsCondition getPodRequiresRestart() {
    return DistributedLogsStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }

}
