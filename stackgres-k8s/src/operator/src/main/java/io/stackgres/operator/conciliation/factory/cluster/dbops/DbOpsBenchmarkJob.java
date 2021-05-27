/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.dbops;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@OpJob("benchmark")
public class DbOpsBenchmarkJob implements JobFactory {

  private final BenchmarkJobDiscoverer jobDiscoverer;

  @Inject
  public DbOpsBenchmarkJob(BenchmarkJobDiscoverer jobDiscoverer) {
    this.jobDiscoverer = jobDiscoverer;
  }

  @Override
  public Job createJob(StackGresClusterContext context, StackGresDbOps dbOps) {

    Map<String, JobFactory> factories = jobDiscoverer.discoverFactories(context);

    final String benchmarkType = Optional.ofNullable(dbOps.getSpec())
        .map(StackGresDbOpsSpec::getBenchmark)
        .map(StackGresDbOpsBenchmark::getType)
        .orElseThrow();

    if (factories.containsKey(benchmarkType)) {
      return factories.get(benchmarkType)
          .createJob(context, dbOps);
    } else {
      throw new UnsupportedOperationException("DbOps benchmark benchmarkType "
          + benchmarkType
          + " not implemented!");
    }
  }
}
