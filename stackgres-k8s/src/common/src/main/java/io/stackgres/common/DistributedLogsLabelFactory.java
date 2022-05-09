/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@ApplicationScoped
public class DistributedLogsLabelFactory
    extends AbstractLabelFactoryForCluster<StackGresDistributedLogs> {

  private final LabelMapperForCluster<StackGresDistributedLogs> labelMapper;

  @Inject
  public DistributedLogsLabelFactory(DistributedLogsLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForCluster<StackGresDistributedLogs> labelMapper() {
    return labelMapper;
  }

}
