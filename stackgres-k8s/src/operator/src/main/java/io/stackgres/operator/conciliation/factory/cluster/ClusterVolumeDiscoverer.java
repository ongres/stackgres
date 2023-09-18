/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractVolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;

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
