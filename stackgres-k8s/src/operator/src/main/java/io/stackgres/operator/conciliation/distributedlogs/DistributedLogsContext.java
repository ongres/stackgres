/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface DistributedLogsContext extends GenerationContext<StackGresDistributedLogs> {

  List<OwnerReference> getOwnerReferences();

  List<StackGresCluster> getConnectedClusters();

}
