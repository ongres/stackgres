/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.v14.DistributedLogsLabelFactoryV14;
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
  public Map<String, String> patroniPrimaryLabels(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.patroniPrimaryLabels(resource);
    }
    return super.patroniPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> patroniPrimaryLabelsWithoutScope(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.patroniPrimaryLabelsWithoutScope(resource);
    }
    return super.patroniPrimaryLabelsWithoutScope(resource);
  }

  @Override
  public Map<String, String> patroniClusterLabelsWithoutScope(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.patroniClusterLabelsWithoutScope(resource);
    }
    return super.patroniClusterLabelsWithoutScope(resource);
  }

  @Override
  public Map<String, String> patroniReplicaLabelsWithoutScope(StackGresDistributedLogs resource) {
    if (useV14(resource)) {
      return distributedLogsLabelFactoryV14.patroniReplicaLabelsWithoutScope(resource);
    }
    return super.patroniReplicaLabelsWithoutScope(resource);
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
