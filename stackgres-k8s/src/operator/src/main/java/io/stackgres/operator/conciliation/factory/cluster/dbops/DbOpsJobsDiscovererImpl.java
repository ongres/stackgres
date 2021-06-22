/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.dbops;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;

@ApplicationScoped
public class DbOpsJobsDiscovererImpl extends ResourceDiscoverer<JobFactory>
    implements DbOpsJobsDiscoverer {

  @Inject
  public DbOpsJobsDiscovererImpl(
      @OpJob
      Instance<JobFactory> instance) {
    init(instance);
  }

  @Override
  public Map<String, JobFactory> discoverFactories(StackGresClusterContext context) {

    StackGresVersion version = StackGresVersion.getClusterStackGresVersion(context.getSource());

    return resourceHub.get(version).stream().collect(Collectors
        .toMap(dbop -> dbop.getClass().getAnnotation(OpJob.class).value(), Function.identity()));
  }
}
