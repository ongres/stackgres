/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels.v14;

import java.util.Map;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.DistributedLogsLabelMapper;
import org.jetbrains.annotations.NotNull;

public class DistributedLogsLabelFactoryV14
    extends AbstractLabelFactoryForCluster<StackGresDistributedLogs> {

  private final DistributedLogsLabelMapper labelMapper;

  public DistributedLogsLabelFactoryV14(DistributedLogsLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public Map<String, String> patroniClusterLabelsWithoutScope(StackGresDistributedLogs resource) {
    return patroniClusterLabels(resource);
  }

  @Override
  public Map<String, String> patroniPrimaryLabelsWithoutScope(StackGresDistributedLogs resource) {
    return patroniPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> patroniReplicaLabelsWithoutScope(StackGresDistributedLogs resource) {
    return patroniReplicaLabels(resource);
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
