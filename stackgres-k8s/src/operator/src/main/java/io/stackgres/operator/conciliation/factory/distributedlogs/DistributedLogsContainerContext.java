/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;

import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import org.immutables.value.Value;

@Value.Immutable
public interface DistributedLogsContainerContext extends ContainerContext {

  StackGresDistributedLogsContext getDistributedLogsContext();

  List<StackGresClusterInstalledExtension> getInstalledExtensions();

}
