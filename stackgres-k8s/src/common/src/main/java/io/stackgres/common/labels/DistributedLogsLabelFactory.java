/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class DistributedLogsLabelFactory
    extends AbstractLabelFactoryForCluster<StackGresDistributedLogs> {

  private final DistributedLogsLabelMapper labelMapper;

  @Inject
  public DistributedLogsLabelFactory(DistributedLogsLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> clusterLabels(StackGresDistributedLogs resource) {
    return super.clusterLabels(resource);
  }

  @Override
  public Map<String, String> patroniClusterLabels(StackGresDistributedLogs resource) {
    return super.patroniClusterLabels(resource);
  }

  @Override
  public Map<String, String> clusterPrimaryLabels(StackGresDistributedLogs resource) {
    return super.clusterPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> statefulSetPodLabels(StackGresDistributedLogs resource) {
    return super.statefulSetPodLabels(resource);
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(StackGresDistributedLogs resource) {
    return super.scheduledBackupPodLabels(resource);
  }

  @Override
  public Map<String, String> clusterCrossNamespaceLabels(StackGresDistributedLogs resource) {
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

}
