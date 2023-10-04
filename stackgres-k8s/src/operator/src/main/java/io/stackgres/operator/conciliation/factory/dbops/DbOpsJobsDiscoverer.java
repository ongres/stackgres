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
    extends AbstractDiscoverer<DbOpsJobFactory> {

  @Inject
  public DbOpsJobsDiscoverer(
      @DbOpsJob Instance<DbOpsJobFactory> instance) {
    super(instance);
  }

  public Map<String, DbOpsJobFactory> discoverFactories(StackGresDbOpsContext context) {
    return hub.get(context.getVersion()).stream()
        .collect(Collectors.toMap(
            dbop -> getAnnotation(dbop, DbOpsJob.class).value(),
            Function.identity()));
  }
}
