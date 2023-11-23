/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Predicates;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ShardedDbOpsMajorVersionUpgradeMutator implements ShardedDbOpsMutator {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Inject
  public ShardedDbOpsMajorVersionUpgradeMutator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public StackGresShardedDbOps mutate(ShardedDbOpsReview review, StackGresShardedDbOps resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }

    if (resource.getSpec() != null
        && resource.getSpec().getSgShardedCluster() != null) {
      Optional<StackGresShardedCluster> foundCluster = clusterFinder.findByNameAndNamespace(
          resource.getSpec().getSgShardedCluster(),
          resource.getMetadata().getNamespace());
      if (resource.getSpec().getMajorVersionUpgrade() != null
          && resource.getSpec().getMajorVersionUpgrade().getPostgresVersion() != null) {
        if (foundCluster.filter(this::clusterHasBackups).isPresent()) {
          final StackGresShardedCluster cluster = foundCluster.orElseThrow();
          final List<String> backupPaths =
              resource.getSpec().getMajorVersionUpgrade().getBackupPaths();
          if (backupPaths == null) {
            final List<String> backupsPaths = getBackupPaths(resource, cluster);
            resource.getSpec().getMajorVersionUpgrade().setBackupPaths(backupsPaths);
          } else if (backupPaths.size() < getNumberOfClusters(cluster)) {
            final List<String> backupsPaths = getBackupPaths(resource, cluster);
            resource.getSpec().getMajorVersionUpgrade().setBackupPaths(Seq.seq(backupPaths)
                .append(backupsPaths.stream().skip(backupPaths.size()))
                .toList());
          }
        }
      }
    }

    return resource;
  }

  private boolean clusterHasBackups(StackGresShardedCluster cluster) {
    var configurations = Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getConfigurations);
    return configurations.map(StackGresShardedClusterConfigurations::getBackups)
        .filter(Predicates.not(List::isEmpty))
        .isPresent();
  }

  private List<String> getBackupPaths(
      StackGresShardedDbOps dbOps, StackGresShardedCluster cluster) {
    final String postgresVersion = dbOps.getSpec()
        .getMajorVersionUpgrade().getPostgresVersion();
    final String postgresFlavor = cluster.getSpec()
        .getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
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
