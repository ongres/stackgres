/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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
  public Map<String, JobFactory> discoverFactories(StackGresDbOpsContext context) {
    return resourceHub.get(context.getVersion()).stream().collect(Collectors
        .toMap(dbop -> getAnnotation(dbop, OpJob.class).value(), Function.identity()));
  }
}
