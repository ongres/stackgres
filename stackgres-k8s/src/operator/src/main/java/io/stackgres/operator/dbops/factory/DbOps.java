/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.factory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.common.ImmutableStackGresDbOpsContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresDbOpsContext;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DbOps
    implements SubResourceStreamFactory<HasMetadata, StackGresGeneratorContext> {

  private final DbOpsBenchmark benchmark;
  private final DbOpsVacuumJob vacuumJob;
  private final DbOpsRepackJob repackJob;
  private final DbOpsRole role;

  @Inject
  public DbOps(DbOpsBenchmark benchmark,
      DbOpsVacuumJob vacuumJob,
      DbOpsRepackJob repackJob,
      DbOpsRole role) {
    this.benchmark = benchmark;
    this.vacuumJob = vacuumJob;
    this.repackJob = repackJob;
    this.role = role;
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final Instant now = Instant.now();
    return Seq.of(context.getClusterContext())
        .map(StackGresClusterContext::getDbOps)
        .flatMap(List::stream)
        .flatMap(dbOps -> streamResources(context, dbOps, now))
        .append(role.streamResources(context));
  }

  private Stream<HasMetadata> streamResources(StackGresGeneratorContext context,
      StackGresDbOps dbOps, Instant now) {
    StackGresDbOpsContext dbOpsContext = ImmutableStackGresDbOpsContext.builder()
        .from(context.getClusterContext())
        .ownerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(dbOps)))
        .currentDbOps(dbOps)
        .build();
    if (isToRunAfter(dbOps, now)) {
      return Seq.empty();
    }

    if (Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::isOpBenchmark)
        .orElse(false)) {
      return ResourceGenerator
          .with(dbOpsContext)
          .of(HasMetadata.class)
          .append(benchmark)
          .stream();
    }

    if (Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::isOpVacuum)
        .orElse(false)) {
      return ResourceGenerator
          .with(dbOpsContext)
          .of(HasMetadata.class)
          .append(vacuumJob)
          .stream();
    }

    if (Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::isOpRepack)
        .orElse(false)) {
      return ResourceGenerator
          .with(dbOpsContext)
          .of(HasMetadata.class)
          .append(repackJob)
          .stream();
    }

    throw new UnsupportedOperationException("DbOps "
        + dbOps.getSpec().getOp() + " not implemented!");
  }

  public static Boolean isToRunAfter(StackGresDbOps dbOps, Instant now) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getRunAt)
        .map(Instant::parse)
        .map(runAt -> runAt.isBefore(now))
        .orElse(false);
  }

  public static boolean isCompleted(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
    .map(StackGresDbOps::getStatus)
    .map(StackGresDbOpsStatus::getConditions)
    .stream()
    .flatMap(List::stream)
    .anyMatch(DbOpsStatusCondition.DB_OPS_COMPLETED::isCondition);
  }

  public static boolean isMaxRetriesReached(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
    .map(StackGresDbOps::getStatus)
    .map(StackGresDbOpsStatus::getOpRetries)
    .orElse(0) >= Optional.of(dbOps)
    .map(StackGresDbOps::getSpec)
    .map(StackGresDbOpsSpec::getMaxRetries)
    .orElse(0);
  }

  public static boolean isFailed(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .anyMatch(Predicates.and(
            DbOpsStatusCondition.Type.FAILED::isCondition,
            DbOpsStatusCondition.Status.TRUE::isCondition));
  }

}
