/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.stream.Collectors;

import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterEnvironmentVariablesFactoryDiscovererImpl
    extends ResourceDiscoverer<ClusterEnvironmentVariablesFactory<ClusterContext>>
    implements ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext> {

  @Inject
  public ClusterEnvironmentVariablesFactoryDiscovererImpl(
      @Any Instance<ClusterEnvironmentVariablesFactory<ClusterContext>> instance) {
    init(instance);
  }

  @Override
  public List<ClusterEnvironmentVariablesFactory<ClusterContext>> discoverFactories(
      ClusterContext context) {
    StackGresVersion clusterVersion = StackGresVersion.getStackGresVersion(context.getCluster());
    return resourceHub.get(clusterVersion).stream()
        .collect(Collectors.toUnmodifiableList());
  }
}
