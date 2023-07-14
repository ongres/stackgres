/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractVolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterVolumeDiscoverer
    extends AbstractVolumeDiscoverer<StackGresClusterContext> {

  @Inject
  public ClusterVolumeDiscoverer(
      @Any Instance<VolumeFactory<StackGresClusterContext>> instance) {
    super(instance);
  }

  @Override
  protected boolean isSelected(VolumeFactory<StackGresClusterContext> generator) {
    return generator.kind() == StackGresGroupKind.CLUSTER;
  }

}
