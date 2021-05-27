/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.dbops;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class DbOpsJobsGenerator implements ResourceGenerator<StackGresClusterContext> {

  private final DbOpsJobsDiscoverer jobsDiscoverer;

  @Inject
  public DbOpsJobsGenerator(DbOpsJobsDiscoverer jobsDiscoverer) {
    this.jobsDiscoverer = jobsDiscoverer;
  }

  public static Boolean isToRunAfter(StackGresDbOps dbOps, Instant now) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getRunAt)
        .map(Instant::parse)
        .map(runAt -> !runAt.isBefore(now))
        .orElse(false);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext config) {

    final List<StackGresDbOps> dbOps = config.getDbOps();
    if (!dbOps.isEmpty()) {
      Instant now = Instant.now();
      Map<String, JobFactory> factories = jobsDiscoverer.discoverFactories(config);
      return dbOps.stream()
          .filter(dbOp -> !isToRunAfter(dbOp, now))
          .map(dbOp -> {
            JobFactory jobFactory = factories.get(dbOp.getSpec().getOp());
            if (jobFactory == null) {
              throw new UnsupportedOperationException("DbOps "
                  + dbOp.getSpec().getOp() + " not implemented!");
            }
            return jobFactory.createJob(config, dbOp);
          });
    }
    return Stream.of();
  }
}
