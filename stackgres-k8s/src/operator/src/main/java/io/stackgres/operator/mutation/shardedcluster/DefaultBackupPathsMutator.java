/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.operator.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultBackupPathsMutator implements ShardedClusterMutator {

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
        .forEach(backup -> {
          if (backup.getPaths() == null) {
            final List<String> backupsPaths = getBackupPaths(resource);
            backup.setPaths(backupsPaths);
          } else if (backup.getPaths().size() < getNumberOfClusters(resource)) {
            final List<String> backupsPaths = getBackupPaths(resource);
            backup.setPaths(Seq.seq(backup.getPaths())
                .append(backupsPaths.stream().skip(backup.getPaths().size()))
                .toList());
          }
        });
    return resource;
  }

  private List<String> getBackupPaths(final StackGresShardedCluster cluster) {
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster).getMajorVersion(postgresVersion);
    return Seq.range(0, getNumberOfClusters(cluster))
        .map(index -> BackupStorageUtil.getPath(
            cluster.getMetadata().getNamespace(),
            StackGresShardedClusterForCitusUtil.getClusterName(cluster, index),
            postgresMajorVersion))
        .toList();
  }

  private int getNumberOfClusters(final StackGresShardedCluster cluster) {
    return cluster.getSpec().getShards().getClusters() + 1;
  }

}
