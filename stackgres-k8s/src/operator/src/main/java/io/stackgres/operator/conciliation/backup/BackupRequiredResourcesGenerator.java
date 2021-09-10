/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.DecorateResource;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresBackup> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(BackupRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private final DecorateResource<StackGresBackupContext> decorator;

  @Inject
  public BackupRequiredResourcesGenerator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      DecorateResource<StackGresBackupContext> decorator) {
    this.clusterFinder = clusterFinder;
    this.backupConfigFinder = backupConfigFinder;
    this.decorator = decorator;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresBackup config) {
    final ObjectMeta metadata = config.getMetadata();
    final String dbOpsName = metadata.getName();
    final String dbOpsNamespace = metadata.getNamespace();

    final StackGresBackupSpec spec = config.getSpec();
    final StackGresCluster cluster = clusterFinder
        .findByNameAndNamespace(spec.getSgCluster(), dbOpsNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGBackup " + dbOpsNamespace + "/" + dbOpsName
                + " target a non existent SGCluster " + spec.getSgCluster()));
    final StackGresBackupConfig backupConfig = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getBackupConfig)
        .map(backupConfigName -> backupConfigFinder
            .findByNameAndNamespace(backupConfigName, dbOpsNamespace)
            .orElseThrow(() -> new IllegalArgumentException(
                "SGBackup " + dbOpsNamespace + "/" + dbOpsName
                    + " target SGCluster " + spec.getSgCluster()
                    + " with a non existent SGBackupConfig " + backupConfigName)))
        .orElseThrow(() -> new IllegalArgumentException(
            "SGBackup " + dbOpsNamespace + "/" + dbOpsName
                + " target SGCluster " + spec.getSgCluster()
                + " without a SGBackupConfig"));

    StackGresBackupContext context = ImmutableStackGresBackupContext.builder()
        .source(config)
        .cluster(cluster)
        .backupConfig(backupConfig)
        .build();

    return decorator.decorateResources(context);
  }

}
