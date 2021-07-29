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

@ApplicationScoped
public class ClusterEnvironmentVariablesFactoryDiscovererImpl
    extends ResourceDiscoverer<ClusterEnvironmentVariablesFactory<StackGresClusterContext>>
    implements ClusterEnvironmentVariablesFactoryDiscoverer<StackGresClusterContext> {

  @Inject
  public ClusterEnvironmentVariablesFactoryDiscovererImpl(
      @Any
          Instance<ClusterEnvironmentVariablesFactory<StackGresClusterContext>> instance) {
    init(instance);
  }

  @Override
  public List<ClusterEnvironmentVariablesFactory<StackGresClusterContext>> discoverFactories(
      StackGresClusterContext context) {
    return resourceHub.get(context.getVersion()).stream()
        .collect(Collectors.toUnmodifiableList());
  }
}
