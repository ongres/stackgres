/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractClusterAnnotationDecorator;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class DistributedLogsAnnotationDecorator
    extends AbstractClusterAnnotationDecorator<StackGresDistributedLogsContext> {

  @Override
  protected Optional<StackGresClusterSpecMetadata> getSpecMetadata(StackGresDistributedLogsContext context) {
    return context.getCluster().map(StackGresCluster::getSpec).map(StackGresClusterSpec::getMetadata);
  }

  @Override
  protected Optional<ObjectMeta> getMetadata(StackGresDistributedLogsContext context) {
    return Optional.of(context.getSource()).map(StackGresDistributedLogs::getMetadata);
  }

  @Override
  protected @NotNull Map<Class<?>, BiConsumer<StackGresDistributedLogsContext, HasMetadata>> getCustomDecorators() {
    return Seq.seq(super.getCustomDecorators())
        .append(Tuple.tuple(StackGresCluster.class, this::decorateCluster))
        .toMap(Tuple2::v1, Tuple2::v2);
  }

  private void decorateCluster(
      @NotNull StackGresDistributedLogsContext context,
      @NotNull HasMetadata cluster) {
    decorateResource(
        cluster,
        Seq.seq(getAllResourcesAnnotations(context))
        .filter(annotation -> !annotation.v1.equals(StackGresContext.VERSION_KEY))
        .toMap(Tuple2::v1, Tuple2::v2));
  }

}
