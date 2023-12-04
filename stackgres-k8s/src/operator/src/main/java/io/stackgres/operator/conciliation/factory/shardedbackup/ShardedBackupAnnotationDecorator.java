/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AbstractShardedClusterAnnotationDecorator;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;

@Singleton
@OperatorVersionBinder
public class ShardedBackupAnnotationDecorator
    extends AbstractShardedClusterAnnotationDecorator<StackGresShardedBackupContext> {

  @Override
  protected StackGresShardedCluster getShardedCluster(StackGresShardedBackupContext context) {
    return context.getShardedCluster();
  }

}
