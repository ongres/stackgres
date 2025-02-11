/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterRestoreBackupContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final ResourceFinder<Secret> secretFinder;
  private final CustomResourceFinder<StackGresBackup> backupFinder;
  private final BackupEnvVarFactory backupEnvVarFactory;

  public ClusterRestoreBackupContextAppender(
      ResourceFinder<Secret> secretFinder,
      CustomResourceFinder<StackGresBackup> backupFinder,
      BackupEnvVarFactory backupEnvVarFactory) {
    this.secretFinder = secretFinder;
    this.backupFinder = backupFinder;
    this.backupEnvVarFactory = backupEnvVarFactory;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<StackGresBackup> restoreBackup = findRestoreBackup(
        cluster,
        cluster.getMetadata().getNamespace());

    final Map<String, Secret> restoreSecrets = restoreBackup
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getSgBackupConfig)
        .map(StackGresBackupConfigSpec::getStorage)
        .stream()
        .flatMap(backupEnvVarFactory::streamStorageSecretReferences)
        .collect(Collectors.groupingBy(Function.<SecretKeySelector>identity()
            .andThen(SecretKeySelector::getName)))
        .entrySet()
        .stream()
        .map(entry -> Tuple.tuple(
            entry.getKey(),
            secretFinder
            .findByNameAndNamespace(
                entry.getKey(),
                cluster.getMetadata().getName())
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + entry.getKey() + " not found for SGBackup "
                    + restoreBackup.get().getMetadata().getName())),
            entry.getValue()))
        .map(t -> {
          t.v3.stream()
              .map(SecretKeySelector::getKey)
              .forEach(key -> {
                if (!t.v2.getData().containsKey(key)) {
                  throw new IllegalArgumentException(
                      "Key " + key + " not found in Secret " + t.v1 + " for SGBackup "
                          + restoreBackup.get().getMetadata().getName());
                }
              });
          return t.limit2();
        })
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));
    contextBuilder
        .restoreBackup(restoreBackup)
        .restoreSecrets(restoreSecrets);
  }

  private Optional<StackGresBackup> findRestoreBackup(
      StackGresCluster cluster,
      final String clusterNamespace) {
    Optional<StackGresBackup> restoreBackup = Optional
        .ofNullable(cluster.getSpec().getInitialData())
        .map(StackGresClusterInitialData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName)
        .flatMap(backupName -> backupFinder.findByNameAndNamespace(backupName, clusterNamespace));
    if (restoreBackup.isPresent()) {
      if (restoreBackup
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getProcess)
          .map(StackGresBackupProcess::getStatus)
          .map(BackupStatus.COMPLETED.status()::equals)
          .map(completed -> !completed)
          .orElse(true)) {
        throw new IllegalArgumentException("Cannot restore from SGBackup "
            + restoreBackup.get().getMetadata().getName()
            + " because it's not Completed");
      }

      String backupMajorVersion = restoreBackup.get()
          .getStatus()
          .getBackupInformation()
          .getPostgresMajorVersion();

      String givenPgVersion = cluster.getSpec()
          .getPostgres().getVersion();
      String givenMajorVersion = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .getMajorVersion(givenPgVersion);

      if (!backupMajorVersion.equals(givenMajorVersion)) {
        throw new IllegalArgumentException("Cannot restore from SGBackup "
            + restoreBackup.get().getMetadata().getName()
            + " because it has been created from a postgres instance"
            + " with version " + backupMajorVersion);
      }
    }
    return restoreBackup;
  }

}
