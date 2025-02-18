/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup.context;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupClusterObjectStorageContextAppender {

  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  public BackupClusterObjectStorageContextAppender(
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder) {
    this.objectStorageFinder = objectStorageFinder;
  }

  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final var foundObjectStorage = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups)
        .map(Collection::stream)
        .flatMap(Stream::findFirst)
        .map(StackGresClusterBackupConfiguration::getSgObjectStorage)
        .or(() -> {
          throw new IllegalArgumentException(
              StackGresCluster.KIND + " " + cluster.getMetadata().getName() + " has no backup configured");
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
