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

  private final LabelFactory<StackGresCluster> clusterLabelFactory;

  private final LabelFactory<StackGresDistributedLogs> distributedLogsLabelFactory;

  @Inject
  public LabelFactoryDelegator(LabelFactory<StackGresCluster> clusterLabelFactory,
                               LabelFactory<StackGresDistributedLogs> distributedLogsLabelFactory) {
    this.clusterLabelFactory = clusterLabelFactory;
    this.distributedLogsLabelFactory = distributedLogsLabelFactory;
  }

  public Map<String, String> patroniClusterLabels(StackGresClusterContext context) {
    return pickFactory(context).patroniClusterLabels(context.getCluster());
  }

  public Map<String, String> genericClusterLabels(StackGresClusterContext context) {
    return pickFactory(context).genericClusterLabels(context.getCluster());
  }

  public Map<String, String> clusterLabels(StackGresClusterContext context) {
    return pickFactory(context).genericClusterLabels(context.getCluster());
  }

  public LabelFactory<?> pickFactory(StackGresClusterContext context) {
    if (context instanceof StackGresDistributedLogsContext) {
      return distributedLogsLabelFactory;
    } else {
      return clusterLabelFactory;
    }
  }
}
