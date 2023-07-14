/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.configuration;

import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.controller.PodLocalControllerContext;
import jakarta.inject.Singleton;

@Singleton
public class DistributedLogsControllerPropertyContext
    implements StackGresPropertyContext<DistributedLogsControllerProperty>,
    PodLocalControllerContext {

  @Override
  public String getClusterName() {
    return getString(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME);
  }

  @Override
  public String getNamespace() {
    return getString(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE);
  }

  @Override
  public String getPodName() {
    return getString(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME);
  }
}
