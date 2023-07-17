/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@OpJob("benchmark")
public class DbOpsBenchmarkJob implements JobFactory {

  private final BenchmarkJobDiscoverer jobDiscoverer;

  @Inject
  public DbOpsBenchmarkJob(BenchmarkJobDiscoverer jobDiscoverer) {
    this.jobDiscoverer = jobDiscoverer;
  }

  @Override
  public Job createJob(StackGresDbOpsContext context) {
    Map<String, JobFactory> factories = jobDiscoverer.discoverFactories(context);

    final String benchmarkType = Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresDbOpsSpec::getBenchmark)
        .map(StackGresDbOpsBenchmark::getType)
        .orElseThrow();

    if (factories.containsKey(benchmarkType)) {
      return factories.get(benchmarkType)
          .createJob(context);
    } else {
      throw new UnsupportedOperationException("DbOps benchmark benchmarkType "
          + benchmarkType
          + " not implemented!");
    }
  }
}
