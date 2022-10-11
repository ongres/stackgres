/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class DbOpsJobsGenerator implements ResourceGenerator<StackGresDbOpsContext> {

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
  public Stream<HasMetadata> generateResource(StackGresDbOpsContext config) {
    Instant now = Instant.now();
    Map<String, JobFactory> factories = jobsDiscoverer.discoverFactories(config);
    return Seq.of(config.getSource())
        .filter(dbOp -> !DbOpsUtil.isAlreadyCompleted(dbOp))
        .filter(dbOp -> !isToRunAfter(dbOp, now))
        .map(dbOp -> {
          JobFactory jobFactory = factories.get(dbOp.getSpec().getOp());
          if (jobFactory == null) {
            throw new UnsupportedOperationException("DbOps "
                + dbOp.getSpec().getOp() + " not implemented!");
          }
          return jobFactory.createJob(config);
        });
  }
}
