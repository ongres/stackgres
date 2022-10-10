/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;

@ApplicationScoped
public class BenchmarkJobDiscovererImpl extends ResourceDiscoverer<JobFactory>
    implements BenchmarkJobDiscoverer {

  @Inject
  public BenchmarkJobDiscovererImpl(@BenchmarkJob Instance<JobFactory> instance) {
    init(instance);
  }

  @Override
  public Map<String, JobFactory> discoverFactories(StackGresDbOpsContext context) {
    return resourceHub.get(context.getVersion()).stream().collect(Collectors
        .toMap(dbop -> getAnnotation(dbop, BenchmarkJob.class).value(),
            Function.identity()));
  }
}
