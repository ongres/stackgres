/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;

import io.stackgres.common.ShardedClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.AbstractDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterEnvironmentVariablesFactoryDiscoverer
    extends AbstractDiscoverer<ShardedClusterEnvironmentVariablesFactory> {

  @Inject
  public ShardedClusterEnvironmentVariablesFactoryDiscoverer(
      @Any Instance<ShardedClusterEnvironmentVariablesFactory> instance) {
    super(instance);
  }

  public List<ShardedClusterEnvironmentVariablesFactory> discoverFactories(
      ShardedClusterContext context) {
    StackGresVersion clusterVersion = StackGresVersion.getStackGresVersion(
        context.getShardedCluster());
    return hub.get(clusterVersion).stream().toList();
  }

}
