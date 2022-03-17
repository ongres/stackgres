/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
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

  private final CustomResourceScanner<StackGresBackup> backupScanner;
  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  private final RequiredResourceDecorator<StackGresBackupContext> decorator;

  @Inject
  public BackupRequiredResourcesGenerator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceScanner<StackGresBackup> backupScanner,
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      RequiredResourceDecorator<StackGresBackupContext> decorator) {
    this.clusterFinder = clusterFinder;
    this.backupConfigFinder = backupConfigFinder;
    this.backupScanner = backupScanner;
    this.objectStorageFinder = objectStorageFinder;
    this.decorator = decorator;
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

    final StackGresCluster cluster = clusterFinder
        .findByNameAndNamespace(clusterName, clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGBackup " + backupNamespace + "/" + backupName
                + " target a non existent SGCluster " + clusterNamespace + "." + clusterName));

    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(backupNamespace);

    var contextBuilder = ImmutableStackGresBackupContext.builder()
        .source(config)
        .cluster(cluster)
        .clusterBackupNamespaces(clusterBackupNamespaces);

    if (isBackupInTheSameSgClusterNamespace(config, clusterNamespace)) {

      final Optional<String> sgBackupConfigurationName = Optional.of(cluster.getSpec())
          .map(StackGresClusterSpec::getConfiguration)
          .map(StackGresClusterConfiguration::getBackupConfig);

      final Optional<String> sgObjectConfigurationName = Optional.of(cluster.getSpec())
          .map(StackGresClusterSpec::getConfiguration)
          .map(StackGresClusterConfiguration::getBackups)
          .filter(bs -> !bs.isEmpty())
          .map(bs -> bs.get(0))
          .map(StackGresClusterBackupConfiguration::getObjectStorage);

      if (sgObjectConfigurationName.isEmpty() && sgBackupConfigurationName.isEmpty()) {
        throw new IllegalArgumentException(
            "SGBackup " + backupNamespace + "/" + backupName
                + " target SGCluster " + spec.getSgCluster()
                + " without a SGObjectStorage or SGBackupConfig"
        );
      }

      sgObjectConfigurationName.ifPresent(osName -> contextBuilder.objectStorage(
          objectStorageFinder.findByNameAndNamespace(osName, backupNamespace)
              .orElseThrow(
                  () -> new IllegalArgumentException(
                      "SGBackup " + backupNamespace + "/" + backupName
                          + " target SGCluster " + spec.getSgCluster()
                          + " with a non existent SGObjectStorage " + osName
                  )
              )
      ));

      sgBackupConfigurationName.ifPresent(bcName -> contextBuilder.backupConfig(
          backupConfigFinder.findByNameAndNamespace(bcName, backupNamespace)
              .orElseThrow(
                  () -> new IllegalArgumentException(
                      "SGBackup " + backupNamespace + "/" + backupName
                          + " target SGCluster " + spec.getSgCluster()
                          + " with a non existent SGBackupConfig " + bcName
                  )
              )));
    }

    return decorator.decorateResources(contextBuilder.build());
  }

  private boolean isBackupInTheSameSgClusterNamespace(
      StackGresBackup backup,
      String clusterNamespace) {
    return Objects.equals(
        backup.getMetadata().getNamespace(),
        clusterNamespace);
  }

  private Set<String> getClusterBackupNamespaces(final String backupNamespace) {
    return backupScanner.getResources()
        .stream()
        .map(Optional::of)
        .map(backup -> backup
            .map(StackGresBackup::getMetadata)
            .map(ObjectMeta::getNamespace))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(((Predicate<String>) backupNamespace::equals).negate())
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet();
  }

}
