/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractVolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsVolumeDiscoverer
    extends AbstractVolumeDiscoverer<StackGresDistributedLogsContext> {

  @Inject
  public DistributedLogsVolumeDiscoverer(
      @Any Instance<VolumeFactory<StackGresDistributedLogsContext>> instance) {
    super(instance);
  }

  @Override
  protected boolean isSelected(VolumeFactory<StackGresDistributedLogsContext> generator) {
    return generator.kind() == StackGresGroupKind.CLUSTER;
  }

}
