/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream.context;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.StreamUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StreamClusterContextAppender
    extends ContextAppender<StackGresStream, Builder> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  public StreamClusterContextAppender(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void appendContext(StackGresStream stream, Builder contextBuilder) {
    if (StreamUtil.isAlreadyCompleted(stream)) {
      return;
    }

    if (!Objects.equals(
        stream.getSpec().getSource().getType(),
        StreamSourceType.SGCLUSTER.toString())) {
      return;
    }

    String clusterName = stream.getSpec().getSource().getSgCluster().getName();
    String streamNamespace = stream.getMetadata().getNamespace();
    Optional<StackGresCluster> foundCluster = clusterFinder
        .findByNameAndNamespace(clusterName, streamNamespace);
    if (foundCluster.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresCluster.KIND + " " + clusterName + " not found");
    }
  }

}
