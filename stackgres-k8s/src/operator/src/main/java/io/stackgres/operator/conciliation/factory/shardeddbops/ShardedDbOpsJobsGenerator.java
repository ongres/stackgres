/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class ShardedDbOpsJobsGenerator implements ResourceGenerator<StackGresShardedDbOpsContext> {

  private final ShardedDbOpsJobsDiscoverer jobsDiscoverer;

  @Inject
  public ShardedDbOpsJobsGenerator(ShardedDbOpsJobsDiscoverer jobsDiscoverer) {
    this.jobsDiscoverer = jobsDiscoverer;
  }

  public static Boolean isToRunAfter(StackGresShardedDbOps dbOps, Instant now) {
    return Optional.of(dbOps)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getRunAt)
        .map(Instant::parse)
        .map(runAt -> !runAt.isBefore(now))
        .orElse(false);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedDbOpsContext config) {
    Instant now = Instant.now();
    Map<String, ShardedDbOpsJobFactory> factories = jobsDiscoverer.discoverFactories(config);
    return Seq.of(config.getSource())
        .filter(dbOp -> !ShardedDbOpsUtil.isAlreadyCompleted(dbOp))
        .filter(dbOp -> !isToRunAfter(dbOp, now))
        .map(dbOp -> {
          ShardedDbOpsJobFactory jobFactory = factories.get(dbOp.getSpec().getOp());
          if (jobFactory == null) {
            throw new UnsupportedOperationException("ShardedDbOps "
                + dbOp.getSpec().getOp() + " not implemented!");
          }
          return jobFactory.createJob(config);
        });
  }
}
