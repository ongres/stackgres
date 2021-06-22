/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;

@ApplicationScoped
public class ClusterStatefulSetVolumeFactoryDiscovererImpl
    extends ResourceDiscoverer<ClusterStatefulSetVolumeFactory<StackGresClusterContext>>
    implements ClusterStatefulSetVolumeFactoryDiscoverer<StackGresClusterContext> {

  @Inject
  public ClusterStatefulSetVolumeFactoryDiscovererImpl(
      @Any
          Instance<ClusterStatefulSetVolumeFactory<StackGresClusterContext>> instance) {
    init(instance);
  }

  @Override
  public List<ClusterStatefulSetVolumeFactory<StackGresClusterContext>> discoverFactories(
      StackGresClusterContext context) {
    StackGresVersion version = StackGresVersion.getClusterStackGresVersion(context.getSource());
    return resourceHub.get(version).stream()
        .collect(Collectors.toUnmodifiableList());
  }
}
