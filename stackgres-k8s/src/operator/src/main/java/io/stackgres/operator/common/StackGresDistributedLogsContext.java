/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresDistributedLogsContext
    extends StackGresClusterContext {

  public abstract StackGresDistributedLogs getDistributedLogs();

  public abstract ImmutableList<StackGresCluster> getConnectedClusters();

}
