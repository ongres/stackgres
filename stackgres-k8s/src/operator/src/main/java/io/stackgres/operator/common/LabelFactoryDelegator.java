/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@ApplicationScoped
public class LabelFactoryDelegator {

  private LabelFactory<StackGresCluster> clusterLabelFactory;

  private LabelFactory<StackGresDistributedLogs> distributedLogsLabelFactory;

  public Map<String, String> patroniClusterLabels(StackGresClusterContext context) {
    if (context instanceof StackGresDistributedLogsContext) {
      return distributedLogsLabelFactory
          .patroniClusterLabels(((StackGresDistributedLogsContext) context).getDistributedLogs());
    } else {
      return clusterLabelFactory
          .patroniClusterLabels(context.getCluster());
    }
  }

  public Map<String, String> genericClusterLabels(StackGresClusterContext context) {
    if (context instanceof StackGresDistributedLogsContext) {
      return distributedLogsLabelFactory
          .genericClusterLabels(((StackGresDistributedLogsContext) context).getDistributedLogs());
    } else {
      return clusterLabelFactory
          .genericClusterLabels(context.getCluster());
    }
  }

  public Map<String, String> clusterLabels(StackGresClusterContext context) {
    return this.genericClusterLabels(context);
  }

  public LabelFactory<?> pickFactory(StackGresClusterContext context) {
    if (context instanceof StackGresDistributedLogsContext) {
      return distributedLogsLabelFactory;
    } else {
      return clusterLabelFactory;
    }
  }

  @Inject
  public void setClusterLabelFactory(LabelFactory<StackGresCluster> clusterLabelFactory) {
    this.clusterLabelFactory = clusterLabelFactory;
  }

  @Inject
  public void setDistributedLogsLabelFactory(
      LabelFactory<StackGresDistributedLogs> distributedLogsLabelFactory) {
    this.distributedLogsLabelFactory = distributedLogsLabelFactory;
  }
}
