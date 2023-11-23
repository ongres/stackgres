/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.operator.conciliation.AbstractDiscoverer;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class BenchmarkJobDiscoverer
    extends AbstractDiscoverer<DbOpsJobFactory>  {

  @Inject
  public BenchmarkJobDiscoverer(@BenchmarkJob Instance<DbOpsJobFactory> instance) {
    super(instance);
  }

  public Map<String, DbOpsJobFactory> discoverFactories(StackGresDbOpsContext context) {
    return hub.get(context.getVersion()).stream()
        .collect(Collectors.toMap(
            dbop -> getAnnotation(dbop, BenchmarkJob.class).value(),
            Function.identity()));
  }
}
