/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;

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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupProcess;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupSpec;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedBackupRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedBackup> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedBackupRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceScanner<StackGresShardedBackup> shardedBackupScanner;
  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  private final ResourceGenerationDiscoverer<StackGresShardedBackupContext> discoverer;

  @Inject
  public ShardedBackupRequiredResourcesGenerator(
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceScanner<StackGresShardedBackup> shardedBackupScanner,
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      ResourceGenerationDiscoverer<StackGresShardedBackupContext> discoverer) {
    this.shardedClusterFinder = shardedClusterFinder;
    this.clusterFinder = clusterFinder;
    this.profileFinder = profileFinder;
    this.shardedBackupScanner = shardedBackupScanner;
    this.objectStorageFinder = objectStorageFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedBackup config) {
    final ObjectMeta metadata = config.getMetadata();
    final String backupName = metadata.getName();
    final String backupNamespace = metadata.getNamespace();
    final StackGresShardedBackupSpec spec = config.getSpec();
    final String clusterName = StackGresUtil.getNameFromRelativeId(spec.getSgShardedCluster());
    final String clusterNamespace = StackGresUtil.getNamespaceFromRelativeId(
        spec.getSgShardedCluster(), backupNamespace);

    final Optional<StackGresShardedCluster> cluster = shardedClusterFinder
        .findByNameAndNamespace(clusterName, clusterNamespace);
    final Optional<StackGresCluster> coordinator = clusterFinder
        .findByNameAndNamespace(
            getCoordinatorClusterName(spec.getSgShardedCluster()), clusterNamespace);
    final Optional<StackGresProfile> profile = cluster
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getSgInstanceProfile)
        .flatMap(profileName -> profileFinder
            .findByNameAndNamespace(profileName, clusterNamespace));

    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(backupNamespace);

    var contextBuilder = ImmutableStackGresShardedBackupContext.builder()
        .source(config)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(profile)
        .clusterBackupNamespaces(clusterBackupNamespaces);

    if (cluster.isPresent()
        && isShardedBackupInTheSameSgClusterNamespace(config, clusterNamespace)
        && !isShardedBackupFinished(config)) {
      final var specConfiguration = cluster
          .map(StackGresShardedCluster::getSpec)
          .map(StackGresShardedClusterSpec::getConfigurations);

      final Optional<String> sgObjectStorageName = specConfiguration
          .map(StackGresShardedClusterConfigurations::getBackups)
          .map(Collection::stream)
          .flatMap(Stream::findFirst)
          .map(StackGresShardedClusterBackupConfiguration::getSgObjectStorage);

      if (sgObjectStorageName.isEmpty()) {
        throw new IllegalArgumentException(
            "SGShardedBackup " + backupNamespace + "." + backupName
                + " target SGShardedCluster " + spec.getSgShardedCluster()
                + " without a SGObjectStorage");
      }

      sgObjectStorageName.ifPresent(osName -> contextBuilder.objectStorage(
          objectStorageFinder.findByNameAndNamespace(osName, backupNamespace)
              .orElseThrow(
                  () -> new IllegalArgumentException(
                      "SGShardedBackup " + backupNamespace + "." + backupName
                          + " target SGShardedCluster " + spec.getSgShardedCluster()
                          + " with a non existent SGObjectStorage " + osName))));
    }

    return discoverer.generateResources(contextBuilder.build());
  }

  private boolean isShardedBackupInTheSameSgClusterNamespace(
      StackGresShardedBackup backup, String clusterNamespace) {
    return Objects.equals(backup.getMetadata().getNamespace(), clusterNamespace);
  }

  private boolean isShardedBackupFinished(StackGresShardedBackup backup) {
    return Optional.of(backup)
        .map(StackGresShardedBackup::getStatus)
        .map(StackGresShardedBackupStatus::getProcess)
        .map(StackGresShardedBackupProcess::getStatus)
        .map(ShardedBackupStatus::fromStatus)
        .filter(List.of(ShardedBackupStatus.COMPLETED, ShardedBackupStatus.FAILED)::contains)
        .isPresent();
  }

  private Set<String> getClusterBackupNamespaces(final String backupNamespace) {
    return shardedBackupScanner.getResources()
        .stream()
        .map(Optional::of)
        .filter(backup -> backup
            .map(StackGresShardedBackup::getSpec)
            .map(StackGresShardedBackupSpec::getSgShardedCluster)
            .map(StackGresUtil::isRelativeIdNotInSameNamespace)
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
