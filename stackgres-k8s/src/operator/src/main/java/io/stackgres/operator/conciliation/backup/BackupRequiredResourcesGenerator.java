/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresBackup> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(BackupRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private final CustomResourceScanner<StackGresBackup> backupScanner;
  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  private final ResourceGenerationDiscoverer<StackGresBackupContext> discoverer;

  @Inject
  public BackupRequiredResourcesGenerator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceScanner<StackGresBackup> backupScanner,
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      ResourceGenerationDiscoverer<StackGresBackupContext> discoverer) {
    this.clusterFinder = clusterFinder;
    this.profileFinder = profileFinder;
    this.backupConfigFinder = backupConfigFinder;
    this.backupScanner = backupScanner;
    this.objectStorageFinder = objectStorageFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresBackup config) {
    final ObjectMeta metadata = config.getMetadata();
    final String backupName = metadata.getName();
    final String backupNamespace = metadata.getNamespace();
    final StackGresBackupSpec spec = config.getSpec();
    final String clusterName = StackGresUtil.getNameFromRelativeId(spec.getSgCluster());
    final String clusterNamespace = StackGresUtil.getNamespaceFromRelativeId(
        spec.getSgCluster(), backupNamespace);

    final Optional<StackGresCluster> cluster = clusterFinder
        .findByNameAndNamespace(clusterName, clusterNamespace);
    final Optional<StackGresProfile> profile = cluster
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getResourceProfile)
        .flatMap(profileName -> profileFinder
            .findByNameAndNamespace(profileName, clusterNamespace));

    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(backupNamespace);

    var contextBuilder = ImmutableStackGresBackupContext.builder()
        .source(config)
        .foundCluster(cluster)
        .foundProfile(profile)
        .clusterBackupNamespaces(clusterBackupNamespaces);

    if (cluster.isPresent()
        && isBackupInTheSameSgClusterNamespace(config, clusterNamespace)
        && !isBackupFinished(config)) {
      final var specConfiguration = cluster
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getConfiguration);

      final Optional<String> sgBackupConfigurationName = specConfiguration
          .map(StackGresClusterConfiguration::getBackupConfig);

      final Optional<String> sgObjectStorageName = specConfiguration
          .map(StackGresClusterConfiguration::getBackups)
          .map(Collection::stream)
          .flatMap(Stream::findFirst)
          .map(StackGresClusterBackupConfiguration::getObjectStorage);

      if (sgObjectStorageName.isEmpty() && sgBackupConfigurationName.isEmpty()) {
        throw new IllegalArgumentException(
            "SGBackup " + backupNamespace + "." + backupName
                + " target SGCluster " + spec.getSgCluster()
                + " without a SGObjectStorage or SGBackupConfig");
      }

      sgObjectStorageName.ifPresent(osName -> contextBuilder.objectStorage(
          objectStorageFinder.findByNameAndNamespace(osName, backupNamespace)
              .orElseThrow(
                  () -> new IllegalArgumentException(
                      "SGBackup " + backupNamespace + "." + backupName
                          + " target SGCluster " + spec.getSgCluster()
                          + " with a non existent SGObjectStorage " + osName))));

      sgBackupConfigurationName.ifPresent(bcName -> contextBuilder.backupConfig(
          backupConfigFinder.findByNameAndNamespace(bcName, backupNamespace)
              .orElseThrow(
                  () -> new IllegalArgumentException(
                      "SGBackup " + backupNamespace + "." + backupName
                          + " target SGCluster " + spec.getSgCluster()
                          + " with a non existent SGBackupConfig " + bcName))));
    }

    return discoverer.generateResources(contextBuilder.build());
  }

  private boolean isBackupInTheSameSgClusterNamespace(
      StackGresBackup backup, String clusterNamespace) {
    return Objects.equals(backup.getMetadata().getNamespace(), clusterNamespace);
  }

  private boolean isBackupFinished(StackGresBackup backup) {
    return Optional.of(backup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .map(BackupStatus::fromStatus)
        .filter(List.of(BackupStatus.COMPLETED, BackupStatus.FAILED)::contains)
        .isPresent();
  }

  private Set<String> getClusterBackupNamespaces(final String backupNamespace) {
    return backupScanner.getResources()
        .stream()
        .map(Optional::of)
        .filter(backup -> backup
            .map(StackGresBackup::getSpec)
            .map(StackGresBackupSpec::getSgCluster)
            .map(StackGresUtil::isRelativeIdNotInSameNamespace)
            .orElse(false))
        .map(backup -> backup
            .map(StackGresBackup::getMetadata)
            .map(ObjectMeta::getNamespace))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(Predicate.not(backupNamespace::equals))
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet();
  }

}
