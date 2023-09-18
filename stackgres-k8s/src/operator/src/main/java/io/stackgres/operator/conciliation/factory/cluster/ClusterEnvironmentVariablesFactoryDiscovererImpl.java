/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.AbstractDiscoverer;

@ApplicationScoped
public class ClusterEnvironmentVariablesFactoryDiscovererImpl
    extends AbstractDiscoverer<ClusterEnvironmentVariablesFactory<ClusterContext>>
    implements ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext> {

  @Inject
  public ClusterEnvironmentVariablesFactoryDiscovererImpl(
      @Any Instance<ClusterEnvironmentVariablesFactory<ClusterContext>> instance) {
    super(instance);
  }

  @Override
  public List<ClusterEnvironmentVariablesFactory<ClusterContext>> discoverFactories(
      ClusterContext context) {
    StackGresVersion clusterVersion = StackGresVersion.getStackGresVersion(context.getCluster());
    return hub.get(clusterVersion).stream()
        .collect(Collectors.toUnmodifiableList());
  }
}
