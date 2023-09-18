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

import io.stackgres.operator.conciliation.AbstractDiscoverer;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;

@ApplicationScoped
public class DbOpsJobsDiscoverer
    extends AbstractDiscoverer<JobFactory> {

  @Inject
  public DbOpsJobsDiscoverer(
      @OpJob Instance<JobFactory> instance) {
    super(instance);
  }

  public Map<String, JobFactory> discoverFactories(StackGresDbOpsContext context) {
    return hub.get(context.getVersion()).stream()
        .collect(Collectors.toMap(
            dbop -> getAnnotation(dbop, OpJob.class).value(),
            Function.identity()));
  }
}
