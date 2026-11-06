/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupSpec;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedBackupClusterBackupNamespacesContextAppender
    extends ContextAppender<StackGresShardedBackup, Builder> {

  private final CustomResourceScanner<StackGresShardedBackup> backupScanner;

  public ShardedBackupClusterBackupNamespacesContextAppender(
      CustomResourceScanner<StackGresShardedBackup> backupScanner) {
    this.backupScanner = backupScanner;
  }

  @Override
  public void appendContext(StackGresShardedBackup backup, Builder contextBuilder) {
    final String clusterNamespace = StackGresUtil.getNamespaceFromRelativeId(
        backup.getSpec().getSgShardedCluster(), backup.getMetadata().getNamespace());
    final String clusterName = StackGresUtil.getNameFromRelativeId(
        backup.getSpec().getSgShardedCluster());
    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(
        backup.getMetadata().getNamespace(),
        clusterNamespace + "." + clusterName);
    contextBuilder.clusterBackupNamespaces(clusterBackupNamespaces);
  }

  // Only the namespaces that hold a backup of this cluster are listed: the job that creates a
  // backup lists the backups of each of them and keeps the ones of this cluster, so a namespace
  // that holds a backup of another cluster contributes nothing, and the permission to list the
  // backups of a namespace is only granted to the cluster the backups it holds belong to, so
  // listing it fails with Forbidden and the job retries it for the whole backoff of retry.
  private Set<String> getClusterBackupNamespaces(
      final String backupNamespace, final String clusterRelativeId) {
    return backupScanner.getResources()
        .stream()
        .map(Optional::of)
        .filter(backup -> backup
            .map(StackGresShardedBackup::getSpec)
            .map(StackGresShardedBackupSpec::getSgShardedCluster)
            .map(clusterRelativeId::equals)
            .orElse(false))
        .map(backup -> backup
            .map(StackGresShardedBackup::getMetadata)
            .map(ObjectMeta::getNamespace))
        .flatMap(Optional::stream)
        .filter(Predicate.not(backupNamespace::equals))
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet();
  }

}
