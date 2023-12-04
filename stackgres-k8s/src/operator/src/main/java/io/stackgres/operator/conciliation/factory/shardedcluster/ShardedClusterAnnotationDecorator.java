/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AbstractShardedClusterAnnotationDecorator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;

@Singleton
@OperatorVersionBinder
public class ShardedClusterAnnotationDecorator
    extends AbstractShardedClusterAnnotationDecorator<StackGresShardedClusterContext> {

  @Override
  protected StackGresShardedCluster getShardedCluster(StackGresShardedClusterContext context) {
    return context.getShardedCluster();
  }

}
