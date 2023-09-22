/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.AbstractDiscoverer;

@ApplicationScoped
public class ClusterEnvironmentVariablesFactoryDiscoverer
    extends AbstractDiscoverer<ClusterEnvironmentVariablesFactory> {

  @Inject
  public ClusterEnvironmentVariablesFactoryDiscoverer(
      @Any Instance<ClusterEnvironmentVariablesFactory> instance) {
    super(instance);
  }

  public List<ClusterEnvironmentVariablesFactory> discoverFactories(
      ClusterContext context) {
    StackGresVersion clusterVersion = StackGresVersion.getStackGresVersion(context.getCluster());
    return hub.get(clusterVersion).stream().toList();
  }

}
