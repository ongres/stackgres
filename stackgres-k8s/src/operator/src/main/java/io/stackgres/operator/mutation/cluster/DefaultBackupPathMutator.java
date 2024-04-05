/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.time.Instant;
import java.util.Optional;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultBackupPathMutator implements ClusterMutator {

  private final Instant defaultTimestamp;

  @Inject
  public DefaultBackupPathMutator() {
    this.defaultTimestamp = null;
  }

  DefaultBackupPathMutator(Instant defaultTimestamp) {
    this.defaultTimestamp = defaultTimestamp;
  }

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    Optional.ofNullable(resource.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getBackups)
        .map(Seq::seq)
        .orElse(Seq.of())
        .zipWithIndex()
        .forEach(backup -> {
          if (backup.v1.getPath() == null) {
            final String backupsPath = Optional.ofNullable(review.getRequest().getOldObject())
                .map(oldResource -> oldResource.getSpec().getConfigurations())
                .map(StackGresClusterConfigurations::getBackups)
                .map(oldBackups -> oldBackups.get(backup.v2.intValue()))
                .map(StackGresClusterBackupConfiguration::getPath)
                .orElseGet(() -> getDefaultBackupPath(resource));
            backup.v1.setPath(backupsPath);
          }
        });
    return resource;
  }

  private String getDefaultBackupPath(final StackGresCluster cluster) {
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster).getMajorVersion(postgresVersion);
    Instant timestamp = Optional.ofNullable(defaultTimestamp).orElse(Instant.now());
    return BackupStorageUtil.getPath(
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName(),
        timestamp,
        postgresMajorVersion);
  }

}
