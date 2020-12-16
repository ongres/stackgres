/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.factory;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.operator.common.StackGresDbOpsContext;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

@ApplicationScoped
public class DbOpsBenchmark
    implements SubResourceStreamFactory<HasMetadata, StackGresDbOpsContext> {

  private final PgbenchJob pgbench;

  @Inject
  public DbOpsBenchmark(PgbenchJob pgbench) {
    this.pgbench = pgbench;
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresDbOpsContext context) {
    if (Optional.of(context.getCurrentDbOps())
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getBenchmark)
        .map(StackGresDbOpsBenchmark::isTypePgBench)
        .orElse(false)) {
      return ResourceGenerator
          .with(context)
          .of(HasMetadata.class)
          .append(pgbench)
          .stream();
    }

    throw new UnsupportedOperationException("DbOps benchmark type "
        + context.getCurrentDbOps().getSpec().getBenchmark().getType()
        + " not implemented!");
  }

}
