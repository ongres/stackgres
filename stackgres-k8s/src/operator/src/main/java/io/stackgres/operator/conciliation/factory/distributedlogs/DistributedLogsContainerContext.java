/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import org.immutables.value.Value;

@Value.Immutable
public interface DistributedLogsContainerContext extends ContainerContext {

  DistributedLogsContext getDistributedLogsContext();

  Map<String, Volume> availableVolumes();

  String getDataVolumeName();

}
