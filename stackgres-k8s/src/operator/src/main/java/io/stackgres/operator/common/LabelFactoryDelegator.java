/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@ApplicationScoped
public class LabelFactoryDelegator {

  private LabelFactoryForCluster<StackGresCluster> clusterLabelFactory;

  private LabelFactoryForCluster<StackGresDistributedLogs> distributedLogsLabelFactory;

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
          .genericLabels(((StackGresDistributedLogsContext) context).getDistributedLogs());
    } else {
      return clusterLabelFactory
          .genericLabels(context.getCluster());
    }
  }

  public Map<String, String> clusterLabels(StackGresClusterContext context) {
    return this.genericClusterLabels(context);
  }

  public LabelFactoryForCluster<?> pickFactory(StackGresClusterContext context) {
    if (context instanceof StackGresDistributedLogsContext) {
      return distributedLogsLabelFactory;
    } else {
      return clusterLabelFactory;
    }
  }

  @Inject
  public void setClusterLabelFactory(LabelFactoryForCluster<StackGresCluster> clusterLabelFactory) {
    this.clusterLabelFactory = clusterLabelFactory;
  }

  @Inject
  public void setDistributedLogsLabelFactory(
      LabelFactoryForCluster<StackGresDistributedLogs> distributedLogsLabelFactory) {
    this.distributedLogsLabelFactory = distributedLogsLabelFactory;
  }
}
