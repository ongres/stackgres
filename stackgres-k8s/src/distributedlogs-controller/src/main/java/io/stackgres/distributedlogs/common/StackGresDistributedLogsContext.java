/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.common;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.extension.ExtensionReconciliatorContext;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresDistributedLogsContext
    implements ResourceHandlerContext, ExtensionReconciliatorContext {

  @Override
  public abstract StackGresCluster getCluster();

  public abstract StackGresDistributedLogs getDistributedLogs();

  @Override
  public abstract ImmutableList<StackGresClusterInstalledExtension> getExtensions();

}
