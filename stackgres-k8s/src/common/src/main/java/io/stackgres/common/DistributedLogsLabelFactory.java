/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@ApplicationScoped
public class DistributedLogsLabelFactory extends AbstractLabelFactory<StackGresDistributedLogs> {

  private final LabelMapper<StackGresDistributedLogs> labelMapper;

  public DistributedLogsLabelFactory(LabelMapper<StackGresDistributedLogs> labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapper<StackGresDistributedLogs> getLabelMapper() {
    return labelMapper;
  }

}
