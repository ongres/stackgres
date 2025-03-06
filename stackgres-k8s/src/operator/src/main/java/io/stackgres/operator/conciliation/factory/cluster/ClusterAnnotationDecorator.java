/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractClusterAnnotationDecorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ClusterAnnotationDecorator
    extends AbstractClusterAnnotationDecorator<StackGresClusterContext> {

  @Override
  protected Optional<StackGresClusterSpecMetadata> getSpecMetadata(StackGresClusterContext context) {
    return Optional.of(context.getSource()).map(StackGresCluster::getSpec).map(StackGresClusterSpec::getMetadata);
  }

  @Override
  protected Optional<ObjectMeta> getMetadata(StackGresClusterContext context) {
    return Optional.of(context.getSource()).map(StackGresCluster::getMetadata);
  }

}
