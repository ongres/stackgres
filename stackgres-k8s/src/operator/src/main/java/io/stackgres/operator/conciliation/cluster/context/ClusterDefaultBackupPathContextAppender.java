/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterDefaultBackupPathContextAppender {

  private final Instant defaultTimestamp;

  @Inject
  public ClusterDefaultBackupPathContextAppender() {
    this.defaultTimestamp = null;
  }

  ClusterDefaultBackupPathContextAppender(Instant defaultTimestamp) {
    this.defaultTimestamp = defaultTimestamp;
  }

  public void appendContext(StackGresCluster cluster, Builder contextBuilder, String version) {
    List<String> backupPaths =
        Optional.ofNullable(cluster.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getBackups)
        .map(Seq::seq)
        .orElse(Seq.of())
        .zipWithIndex()
        .map(backup -> {
          return getBackupPath(cluster, version, backup.v1, backup.v2.intValue());
        })
        .toList();

    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }

    if (Objects.equals(backupPaths, cluster.getStatus().getBackupPaths())) {
      return;
    }
    if (backupPaths != null && backupPaths.isEmpty()) {
      return;
    }
    cluster.getStatus().setBackupPaths(backupPaths);
  }

  private String getBackupPath(
      StackGresCluster cluster,
      String version,
      StackGresClusterBackupConfiguration backup,
      int index) {
    if (backup.getPath() == null) {
      final String backupsPath = Optional.ofNullable(cluster.getStatus())
          .map(StackGresClusterStatus::getBackupPaths)
          .filter(backupPaths -> backupPaths.size() > index)
          .map(backupPaths -> backupPaths.get(index))
          .orElseGet(() -> getDefaultBackupPath(cluster, version));
      return backupsPath;
    }

    return backup.getPath();
  }

  private String getDefaultBackupPath(StackGresCluster cluster, String version) {
    final String postgresMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster).getMajorVersion(version);
    Instant timestamp = Optional.ofNullable(defaultTimestamp).orElse(Instant.now());
    return BackupStorageUtil.getPath(
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName(),
        timestamp,
        postgresMajorVersion);
  }

}
