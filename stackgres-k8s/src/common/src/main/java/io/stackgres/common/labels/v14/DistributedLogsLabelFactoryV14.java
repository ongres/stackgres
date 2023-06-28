/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels.v14;

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
  public String resourceScope(@NotNull StackGresDistributedLogs resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DistributedLogsLabelMapper labelMapper() {
    return labelMapper;
  }

}
