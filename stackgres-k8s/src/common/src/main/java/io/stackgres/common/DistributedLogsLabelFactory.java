/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
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
  public String resourceScope(@NotNull StackGresDistributedLogs resource) {
    return resourceName(resource);
  }

  @Override
  public DistributedLogsLabelMapper labelMapper() {
    return labelMapper;
  }

}
