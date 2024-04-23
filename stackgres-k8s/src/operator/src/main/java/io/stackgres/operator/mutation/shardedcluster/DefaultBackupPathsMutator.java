/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultBackupPathsMutator implements ShardedClusterMutator {

  private final Instant defaultTimestamp;

  @Inject
  public DefaultBackupPathsMutator() {
    this.defaultTimestamp = null;
  }

  DefaultBackupPathsMutator(Instant defaultTimestamp) {
    this.defaultTimestamp = defaultTimestamp;
  }

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    Optional.ofNullable(resource.getSpec().getConfigurations())
        .map(StackGresShardedClusterConfigurations::getBackups)
        .map(Seq::seq)
        .orElse(Seq.of())
        .zipWithIndex()
        .forEach(backup -> {
          if (backup.v1.getPaths() == null) {
            final List<String> backupsPaths = Optional.ofNullable(review.getRequest().getOldObject())
                .map(oldResource -> oldResource.getSpec().getConfigurations())
                .map(StackGresShardedClusterConfigurations::getBackups)
                .map(oldBackups -> oldBackups.get(backup.v2.intValue()))
                .map(StackGresShardedClusterBackupConfiguration::getPaths)
                .orElseGet(() -> getDefaultBackupPaths(resource));
            backup.v1.setPaths(backupsPaths);
          } 
          
          if (backup.v1.getPaths().size() < getNumberOfClusters(resource)) {
            final List<String> backupsPaths = getDefaultBackupPaths(resource);
            backup.v1.setPaths(Seq.seq(backup.v1.getPaths())
                .append(backupsPaths.stream().skip(backup.v1.getPaths().size()))
                .toList());
          }
        });
    return resource;
  }

  private List<String> getDefaultBackupPaths(final StackGresShardedCluster cluster) {
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster).getMajorVersion(postgresVersion);
    Instant timestamp = Optional.ofNullable(defaultTimestamp).orElse(Instant.now());
    return Seq.range(0, getNumberOfClusters(cluster))
        .map(index -> BackupStorageUtil.getPath(
            cluster.getMetadata().getNamespace(),
            StackGresShardedClusterUtil.getClusterName(cluster, index),
            timestamp,
            postgresMajorVersion))
        .toList();
  }

  private int getNumberOfClusters(final StackGresShardedCluster cluster) {
    return cluster.getSpec().getShards().getClusters() + 1;
  }

}
