/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.backup.Backup;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.patroni.PatroniConfigMap;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

@ApplicationScoped
public class Cluster
    implements SubResourceStreamFactory<HasMetadata,
        StackGresGeneratorContext> {

  private final ClusterStatefulSet clusterStatefulSet;
  private final BackupConfigMap backupConfigMap;
  private final RestoreConfigMap restoreConfigMap;
  private final BackupSecret backupSecret;
  private final RestoreSecret restoreSecret;
  private final BackupCronJob backupCronJob;
  private final Backup backup;

  @Inject
  public Cluster(ClusterStatefulSet clusterStatefulSet, PatroniConfigMap patroniConfigMap,
      BackupConfigMap backupConfigMap, RestoreConfigMap restoreConfigMap,
      BackupSecret backupSecret, RestoreSecret restoreSecret,
      BackupCronJob backupCronJob, Backup backup) {
    super();
    this.clusterStatefulSet = clusterStatefulSet;
    this.backupConfigMap = backupConfigMap;
    this.restoreConfigMap = restoreConfigMap;
    this.backupSecret = backupSecret;
    this.restoreSecret = restoreSecret;
    this.backupCronJob = backupCronJob;
    this.backup = backup;
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    return ResourceGenerator
        .with(context)
        .of(HasMetadata.class)
        .append(clusterStatefulSet)
        .append(backupConfigMap)
        .append(backupSecret)
        .append(backupCronJob)
        .append(backup)
        .append(restoreConfigMap)
        .append(restoreSecret)
        .stream();
  }

}
