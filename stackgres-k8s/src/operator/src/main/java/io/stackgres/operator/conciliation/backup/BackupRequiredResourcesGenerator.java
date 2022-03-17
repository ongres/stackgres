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
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Predicates;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
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

  private final RequiredResourceDecorator<StackGresBackupContext> decorator;

  @Inject
  public BackupRequiredResourcesGenerator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceScanner<StackGresBackup> backupScanner,
      RequiredResourceDecorator<StackGresBackupContext> decorator) {
    this.clusterFinder = clusterFinder;
    this.backupConfigFinder = backupConfigFinder;
    this.backupScanner = backupScanner;
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
    final Optional<StackGresBackupConfig> backupConfig =
        findBackupConfig(config, backupName, backupNamespace, clusterNamespace, cluster);

    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(backupNamespace);

    StackGresBackupContext context = ImmutableStackGresBackupContext.builder()
        .source(config)
        .cluster(cluster)
        .backupConfig(backupConfig)
        .clusterBackupNamespaces(clusterBackupNamespaces)
        .build();

    return decorator.decorateResources(context);
  }

  private Optional<StackGresBackupConfig> findBackupConfig(StackGresBackup config,
      final String backupName, final String backupNamespace, final String clusterNamespace,
      final StackGresCluster cluster) {
    if (!Objects.equals(clusterNamespace, backupNamespace)) {
      return Optional.empty();
    }

    final Optional<StackGresBackupConfig> backupConfig = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getBackupConfig)
        .map(backupConfigName -> backupConfigFinder
            .findByNameAndNamespace(backupConfigName, backupNamespace)
            .orElseThrow(() -> new IllegalArgumentException(
                "SGBackup " + backupNamespace + "/" + backupName
                + " target SGCluster " + config.getSpec().getSgCluster()
                + " with a non existent SGBackupConfig " + backupConfigName)));
    if (backupConfig.isEmpty()) {
      throw new IllegalArgumentException(
          "SGBackup " + backupNamespace + "/" + backupName
          + " target SGCluster " + config.getSpec().getSgCluster()
          + " without a SGBackupConfig");
    }
    return backupConfig;
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
        .filter(Predicates.not(backupNamespace::equals))
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet();
  }

}
