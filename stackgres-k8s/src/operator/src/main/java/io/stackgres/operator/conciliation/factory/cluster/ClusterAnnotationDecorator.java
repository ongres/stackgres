/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractClusterAnnotationDecorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ClusterAnnotationDecorator
    extends AbstractClusterAnnotationDecorator<StackGresClusterContext> {

  @Override
  protected StackGresCluster getCluster(StackGresClusterContext context) {
    return context.getCluster();
  }

}
