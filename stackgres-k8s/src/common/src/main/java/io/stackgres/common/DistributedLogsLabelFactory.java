/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@ApplicationScoped
public class DistributedLogsLabelFactory extends AbstractLabelFactory<StackGresDistributedLogs> {

  private LabelMapper<StackGresDistributedLogs> labelMapper;

  @Override
  public LabelMapper<StackGresDistributedLogs> getLabelMapper() {
    return labelMapper;
  }

  @Inject
  public void setLabelMapper(LabelMapper<StackGresDistributedLogs> labelMapper) {
    this.labelMapper = labelMapper;
  }
}
