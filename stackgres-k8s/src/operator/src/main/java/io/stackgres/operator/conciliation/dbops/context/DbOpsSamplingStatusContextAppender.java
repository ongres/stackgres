/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import java.util.Optional;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmarkStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbench;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSamplingStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.DbOpsUtil;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DbOpsSamplingStatusContextAppender
    extends ContextAppender<StackGresDbOps, Builder> {

  private final CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  public DbOpsSamplingStatusContextAppender(CustomResourceFinder<StackGresDbOps> dbOpsFinder) {
    this.dbOpsFinder = dbOpsFinder;
  }

  @Override
  public void appendContext(StackGresDbOps dbOps, Builder contextBuilder) {
    if (DbOpsUtil.isAlreadyCompleted(dbOps)) {
      contextBuilder.samplingStatus(Optional.empty());
      return;
    }

    final Optional<StackGresDbOpsSamplingStatus> samplingStatus = Optional.of(dbOps.getSpec())
        .map(StackGresDbOpsSpec::getBenchmark)
        .map(StackGresDbOpsBenchmark::getPgbench)
        .map(StackGresDbOpsPgbench::getSamplingSgDbOps)
        .map(samplingDbOpsName -> dbOpsFinder
            .findByNameAndNamespace(
                samplingDbOpsName,
                dbOps.getMetadata().getNamespace())
            .map(StackGresDbOps::getStatus)
            .map(StackGresDbOpsStatus::getBenchmark)
            .map(StackGresDbOpsBenchmarkStatus::getSampling)
            .orElseThrow(() -> new IllegalArgumentException(
                StackGresDbOps.KIND + " " + samplingDbOpsName
                + " was not found or has no has no sampling status")));
    contextBuilder.samplingStatus(samplingStatus);
  }

}
