/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.DbOpsUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class DbOpsJobsGenerator implements ResourceGenerator<StackGresDbOpsContext> {

  private final DbOpsJobsDiscoverer jobsDiscoverer;

  @Inject
  public DbOpsJobsGenerator(DbOpsJobsDiscoverer jobsDiscoverer) {
    this.jobsDiscoverer = jobsDiscoverer;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDbOpsContext config) {
    Instant now = Instant.now();
    Map<String, DbOpsJobFactory> factories = jobsDiscoverer.discoverFactories(config);
    return Seq.of(config.getSource())
        .filter(dbOp -> !DbOpsUtil.ROLLOUT_OPS.contains(dbOp.getSpec().getOp()))
        .filter(dbOp -> !DbOpsUtil.isToRunAfter(dbOp, now))
        .filter(dbOp -> !DbOpsUtil.isAlreadyCompleted(dbOp))
        .map(dbOp -> {
          DbOpsJobFactory jobFactory = factories.get(dbOp.getSpec().getOp());
          if (jobFactory == null) {
            throw new UnsupportedOperationException("DbOps "
                + dbOp.getSpec().getOp() + " not implemented!");
          }
          return jobFactory.createJob(config);
        });
  }

}
