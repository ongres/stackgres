/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedBackupClusterObjectStorageContextAppender {

  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  public ShardedBackupClusterObjectStorageContextAppender(
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder) {
    this.objectStorageFinder = objectStorageFinder;
  }

  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final var foundObjectStorage = Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getBackups)
        .map(Collection::stream)
        .flatMap(Stream::findFirst)
        .map(StackGresShardedClusterBackupConfiguration::getSgObjectStorage)
        .or(() -> {
          throw new IllegalArgumentException(
              StackGresShardedCluster.KIND + " " + cluster.getMetadata().getName() + " has no backup configured");
        })
        .map(objectStorageName -> objectStorageFinder
            .findByNameAndNamespace(
                objectStorageName,
                cluster.getMetadata().getNamespace())
            .orElseThrow(
                () -> new IllegalArgumentException(
                    StackGresObjectStorage.KIND + " " + objectStorageName + " was not found")));
    contextBuilder.objectStorage(foundObjectStorage);
  }

}
