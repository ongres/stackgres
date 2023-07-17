/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutatorWeight;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@MutatorWeight(10)
public class DefaultBackupStorageMigratorMutator implements ClusterMutator {

  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;
  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;
  private final CustomResourceScheduler<StackGresObjectStorage> objectStorageScheduler;

  @Inject
  public DefaultBackupStorageMigratorMutator(
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      CustomResourceScheduler<StackGresObjectStorage> objectStorageScheduler) {
    this.backupConfigFinder = backupConfigFinder;
    this.objectStorageFinder = objectStorageFinder;
    this.objectStorageScheduler = objectStorageScheduler;
  }

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    final StackGresClusterConfiguration configuration = resource.getSpec().getConfiguration();
    if (configuration == null) {
      return resource;
    }

    String objStorage = createObjectStorage(resource);
    if (objStorage != null) {
      StackGresBaseBackupConfig copyBaseBackup = copyBaseBackup(resource);
      StackGresClusterBackupConfiguration cbc = new StackGresClusterBackupConfiguration();
      cbc.setObjectStorage(objStorage);
      cbc.setCompression(copyBaseBackup.getCompression());
      cbc.setCronSchedule(copyBaseBackup.getCronSchedule());
      cbc.setPerformance(copyBaseBackup.getPerformance());
      cbc.setRetention(copyBaseBackup.getRetention());
      if (configuration.getBackupPath() != null) {
        cbc.setPath(configuration.getBackupPath());
      } else {
        final String backupPath = getBackupPath(resource);
        cbc.setPath(backupPath);
      }
      configuration.setBackups(List.of(cbc));
    }
    if (configuration.getBackupConfig() != null) {
      configuration.setBackupConfig(null);
    }
    if (configuration.getBackupPath() != null) {
      configuration.setBackupPath(null);
    }
    return resource;
  }

  private StackGresBaseBackupConfig copyBaseBackup(final StackGresCluster cluster) {
    String backupConfig = cluster.getSpec().getConfiguration().getBackupConfig();
    String namespace = cluster.getMetadata().getNamespace();
    return backupConfigFinder.findByNameAndNamespace(backupConfig, namespace)
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getBaseBackups)
        .orElseGet(StackGresBaseBackupConfig::new);
  }

  private String createObjectStorage(final StackGresCluster cluster) {
    String backupConfigName = cluster.getSpec().getConfiguration().getBackupConfig();
    String namespace = cluster.getMetadata().getNamespace();
    if (backupConfigName != null
        && objectStorageFinder.findByNameAndNamespace(backupConfigName, namespace).isEmpty()) {
      var backupConfigStorage = backupConfigFinder
          .findByNameAndNamespace(backupConfigName, namespace)
          .map(StackGresBackupConfig::getSpec)
          .map(StackGresBackupConfigSpec::getStorage);
      if (backupConfigStorage.isEmpty()) {
        return null;
      }
      backupConfigStorage
          .ifPresent(storage -> {
            final StackGresObjectStorage objStorage = new StackGresObjectStorage();
            objStorage.setMetadata(new ObjectMetaBuilder()
                .withName(backupConfigName)
                .withNamespace(namespace)
                .build());
            objStorage.setSpec(storage);
            objectStorageScheduler.create(objStorage);
          });
    }
    return backupConfigName;
  }

  private String getBackupPath(final StackGresCluster cluster) {
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);
    return BackupStorageUtil.getPath(
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName(),
        postgresMajorVersion);
  }

}
