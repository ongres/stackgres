/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.v14.DistributedLogsLabelFactoryV14;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class DistributedLogsLabelFactory
    extends AbstractLabelFactoryForCluster<StackGresDistributedLogs> {

  private final DistributedLogsLabelMapper labelMapper;

  private final DistributedLogsLabelFactoryV14 distributedLogsLabelFactoryV14;

  @Inject
  public DistributedLogsLabelFactory(DistributedLogsLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
    this.distributedLogsLabelFactoryV14 = new DistributedLogsLabelFactoryV14(labelMapper);
  }

  @Override
  public Map<String, String> clusterLabels(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.clusterLabels(resource);
    }
    return super.clusterLabels(resource);
  }

  @Override
  public Map<String, String> patroniClusterLabels(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.patroniClusterLabels(resource);
    }
    return super.patroniClusterLabels(resource);
  }

  @Override
  public Map<String, String> clusterPrimaryLabels(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.clusterPrimaryLabels(resource);
    }
    return super.clusterPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> statefulSetPodLabels(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.statefulSetPodLabels(resource);
    }
    return super.statefulSetPodLabels(resource);
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.scheduledBackupPodLabels(resource);
    }
    return super.scheduledBackupPodLabels(resource);
  }

  @Override
  public Map<String, String> clusterCrossNamespaceLabels(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.clusterCrossNamespaceLabels(resource);
    }
    return super.clusterCrossNamespaceLabels(resource);
  }

  @Override
  public String resourceScope(@NotNull StackGresDistributedLogs resource) {
    return resourceName(resource);
  }

  @Override
  public DistributedLogsLabelMapper labelMapper() {
    return labelMapper;
  }

  private boolean useV14(StackGresDistributedLogs resource) {
    return StackGresVersion.getStackGresVersion(resource)
        .compareTo(StackGresVersion.V_1_4) <= 0;
  }

}
