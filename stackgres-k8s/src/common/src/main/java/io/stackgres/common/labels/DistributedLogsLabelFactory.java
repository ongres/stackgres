/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsLabelFactory
    extends AbstractLabelFactory<StackGresDistributedLogs>
    implements LabelFactoryForDistributedLogs {

  private final DistributedLogsLabelMapper labelMapper;

  @Inject
  public DistributedLogsLabelFactory(DistributedLogsLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public DistributedLogsLabelMapper labelMapper() {
    return labelMapper;
  }

}
