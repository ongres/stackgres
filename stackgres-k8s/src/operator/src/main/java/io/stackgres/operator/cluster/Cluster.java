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
import io.stackgres.operator.patroni.PatroniConfigEndpoints;
import io.stackgres.operator.patroni.PatroniConfigMap;
import io.stackgres.operator.patroni.PatroniRole;
import io.stackgres.operator.patroni.PatroniSecret;
import io.stackgres.operator.patroni.PatroniServices;
import io.stackgres.operatorframework.resource.factory.ResourceStreamFactory;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class Cluster
    implements ResourceStreamFactory<HasMetadata,
        StackGresGeneratorContext> {

  private final ClusterStatefulSet clusterStatefulSet;
  private final PatroniConfigMap patroniConfigMap;
  private final BackupConfigMap backupConfigMap;
  private final RestoreConfigMap restoreConfigMap;
  private final PatroniSecret patroniSecret;
  private final BackupSecret backupSecret;
  private final RestoreSecret restoreSecret;
  private final BackupCronJob backupCronJob;
  private final Backup backup;
  private final PatroniRole patroniRole;
  private final PatroniServices patroniServices;
  private final PatroniConfigEndpoints patroniConfigEndpoints;

  @Inject
  public Cluster(ClusterStatefulSet clusterStatefulSet, PatroniConfigMap patroniConfigMap,
      BackupConfigMap backupConfigMap, RestoreConfigMap restoreConfigMap,
      PatroniSecret patroniSecret, BackupSecret backupSecret, RestoreSecret restoreSecret,
      BackupCronJob backupCronJob, Backup backup, PatroniRole patroniRole,
      PatroniServices patroniServices, PatroniConfigEndpoints patroniConfigEndpoints) {
    super();
    this.clusterStatefulSet = clusterStatefulSet;
    this.patroniConfigMap = patroniConfigMap;
    this.backupConfigMap = backupConfigMap;
    this.restoreConfigMap = restoreConfigMap;
    this.patroniSecret = patroniSecret;
    this.backupSecret = backupSecret;
    this.restoreSecret = restoreSecret;
    this.backupCronJob = backupCronJob;
    this.backup = backup;
    this.patroniRole = patroniRole;
    this.patroniServices = patroniServices;
    this.patroniConfigEndpoints = patroniConfigEndpoints;
  }

  @Override
  public Stream<HasMetadata> create(StackGresGeneratorContext context) {
    return Seq.<HasMetadata>of()
        .append(patroniRole.create(context))
        .append(patroniSecret.create(context))
        .append(patroniServices.create(context))
        .append(patroniConfigEndpoints.create(context))
        .append(patroniConfigMap.create(context))
        .append(backupConfigMap.create(context))
        .append(backupSecret.create(context))
        .append(restoreConfigMap.create(context))
        .append(restoreSecret.create(context))
        .append(backupCronJob.create(context))
        .append(clusterStatefulSet.create(context))
        .append(backup.create(context));
  }

}
