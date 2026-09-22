/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AbstractShardedClusterMetadataDecorator;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedBackupMetadataDecorator
    extends AbstractShardedClusterMetadataDecorator<StackGresShardedBackupContext> {

  @Override
  protected Optional<StackGresShardedClusterSpecMetadata> getSpecMetadata(
      StackGresShardedBackupContext context) {
    return context.getFoundShardedCluster()
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getMetadata);
  }

  @Override
  protected Optional<ObjectMeta> getMetadata(StackGresShardedBackupContext context) {
    return Optional.of(context.getSource()).map(StackGresShardedBackup::getMetadata);
  }

}
