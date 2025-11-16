/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupInformation;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackupTiming;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationInitialization;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresReplicationInitializationMode;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterReplicationInitializationContextAppender {

  private final ResourceFinder<Secret> secretFinder;
  private final BackupEnvVarFactory backupEnvVarFactory;
  private final CustomResourceScanner<StackGresBackup> backupScanner;
  private final LabelFactoryForCluster labelFactory;

  public ClusterReplicationInitializationContextAppender(
      ResourceFinder<Secret> secretFinder,
      BackupEnvVarFactory backupEnvVarFactory,
      CustomResourceScanner<StackGresBackup> backupScanner,
      LabelFactoryForCluster labelFactory) {
    this.secretFinder = secretFinder;
    this.backupEnvVarFactory = backupEnvVarFactory;
    this.backupScanner = backupScanner;
    this.labelFactory = labelFactory;
  }

  public void appendContext(
      StackGresCluster cluster,
      Builder contextBuilder,
      Optional<StackGresObjectStorage> backupObjectStorage,
      String version) {
    final Optional<Tuple2<StackGresBackup, Map<String, Secret>>> replicationInitializationBackupAndSecrets =
        getReplicationInitializationBackupAndSecrets(cluster, backupObjectStorage, version);
    final Optional<StackGresBackup> replicationInitializationBackupToCreate =
        getReplicationInitializationBackupToCreate(cluster, backupObjectStorage, version);
    contextBuilder
        .replicationInitializationBackup(replicationInitializationBackupAndSecrets
            .map(Tuple2::v1))
        .replicationInitializationBackupToCreate(replicationInitializationBackupToCreate)
        .replicationInitializationSecrets(replicationInitializationBackupAndSecrets
            .map(Tuple2::v2)
            .orElse(Map.of()));
  }

  private Optional<Tuple2<StackGresBackup, Map<String, Secret>>> getReplicationInitializationBackupAndSecrets(
      StackGresCluster cluster,
      Optional<StackGresObjectStorage> backupObjectStorage,
      String version) {
    if (StackGresReplicationInitializationMode.FROM_EXISTING_BACKUP.ordinal()
        > cluster.getSpec().getReplication().getInitializationModeOrDefault().ordinal()) {
      return Optional.empty();
    }

    final String namespace = cluster.getMetadata().getNamespace();
    final var backupNewerThan = Optional.ofNullable(cluster.getSpec().getReplication().getInitialization())
        .map(StackGresClusterReplicationInitialization::getBackupNewerThan)
        .map(Duration::parse)
        .map(Instant.now()::minus);
    final String postgresMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getMajorVersion(version);
    return Seq.seq(backupScanner.getResources(cluster.getMetadata().getNamespace()))
        .filter(backup -> backup.getSpec().getSgCluster().equals(
            cluster.getMetadata().getName()))
        .filter(backup -> BackupStatus.isCompleted(backup))
        .filter(backup -> Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getSgBackupConfig)
            .map(StackGresBackupConfigSpec::getStorage)
            .equals(backupObjectStorage.map(StackGresObjectStorage::getSpec)))
        .filter(backup -> Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getBackupPath)
            .equals(Optional
                .ofNullable(cluster.getStatus().getBackupPaths())
                .map(Collection::stream)
                .flatMap(Stream::findFirst)))
        .filter(backup -> Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getBackupInformation)
            .map(StackGresBackupInformation::getPostgresMajorVersion)
            .filter(postgresMajorVersion::equals)
            .isPresent())
        .filter(backup -> Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getTiming)
            .map(StackGresBackupTiming::getEnd)
            .map(Instant::parse)
            .filter(end -> backupNewerThan.map(end::isAfter).orElse(true))
            .isPresent())
        .sorted(Comparator.comparing((StackGresBackup backup) -> Optional.ofNullable(backup.getStatus())
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getTiming)
            .map(StackGresBackupTiming::getEnd)
            .map(Instant::parse)
            .get())
            .reversed())
        .map(backup -> Tuple.tuple(
            backup,
            Optional.of(backup)
            .map(StackGresBackup::getStatus)
            .map(StackGresBackupStatus::getSgBackupConfig)
            .map(StackGresBackupConfigSpec::getStorage)
            .stream()
            .flatMap(backupEnvVarFactory::streamStorageSecretReferences)
            .map(secretKeySelector -> secretKeySelector.getName())
            .collect(Collectors.groupingBy(Function.identity()))
            .keySet()
            .stream()
            .map(name -> Tuple.tuple(
                name,
                secretFinder.findByNameAndNamespace(name, namespace)))
            .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2))))
        .filter(backupAndFoundSecrets -> backupAndFoundSecrets.v2.values().stream().allMatch(Optional::isPresent))
        .map(backupAndFoundSecrets -> backupAndFoundSecrets.map2(secrets -> secrets
            .entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().get()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
        .skipUntil(backupAndFoundSecrets -> Optional.of(cluster)
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getReplicationInitializationFailedSgBackup)
            .map(backupAndFoundSecrets.v1.getMetadata().getName()::equals)
            .orElse(true))
        .skip(Optional.of(cluster)
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getReplicationInitializationFailedSgBackup)
            .map(ignore -> 1)
            .orElse(0))
        .findFirst();
  }

  private Optional<StackGresBackup> getReplicationInitializationBackupToCreate(
      StackGresCluster cluster,
      Optional<StackGresObjectStorage> backupObjectStorage,
      String version) {
    if (!StackGresReplicationInitializationMode.FROM_NEWLY_CREATED_BACKUP.equals(
        cluster.getSpec().getReplication().getInitializationModeOrDefault())) {
      return Optional.empty();
    }
    final var now = Instant.now();
    final var backupNewerThan = Optional.ofNullable(cluster.getSpec().getReplication().getInitialization())
        .map(StackGresClusterReplicationInitialization::getBackupNewerThan)
        .map(Duration::parse)
        .map(now::minus);
    final String postgresMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getMajorVersion(version);
    return Seq.seq(backupScanner
        .getResourcesWithLabels(
            cluster.getMetadata().getNamespace(),
            labelFactory.replicationInitializationBackupLabels(cluster)))
        .filter(backup -> backup.getSpec().getSgCluster().equals(
            cluster.getMetadata().getName()))
        .filter(backup -> backup.getStatus() == null
            || BackupStatus.isFinished(backup)
            || Optional.ofNullable(backup.getStatus())
            .filter(status -> Optional.of(status)
                .map(StackGresBackupStatus::getSgBackupConfig)
                .map(StackGresBackupConfigSpec::getStorage)
                .equals(backupObjectStorage.map(StackGresObjectStorage::getSpec)))
            .filter(status -> Optional.of(status)
                .map(StackGresBackupStatus::getBackupPath)
                .equals(Optional
                    .ofNullable(cluster.getSpec().getConfigurations().getBackups())
                    .map(Collection::stream)
                    .flatMap(Stream::findFirst)
                    .map(StackGresClusterBackupConfiguration::getPath)))
            .filter(status -> Optional.of(status)
                .map(StackGresBackupStatus::getBackupInformation)
                .map(StackGresBackupInformation::getPostgresMajorVersion)
                .filter(postgresMajorVersion::equals)
                .isPresent())
            .filter(status -> BackupStatus.isCompleted(backup))
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getTiming)
            .map(StackGresBackupTiming::getEnd)
            .map(Instant::parse)
            .or(() -> Optional.of(now))
            .filter(end -> backupNewerThan.map(end::isAfter).orElse(true))
            .isPresent())
        .findFirst();
  }

}
