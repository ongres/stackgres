/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.distributedlogs.v14.DistributedLogsCredentials;
import io.stackgres.operator.initialization.DefaultDistributedLogsPostgresConfigFactory;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class DistributedLogsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDistributedLogs> {

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final DefaultProfileFactory defaultProfileFactory;

  private final DefaultDistributedLogsPostgresConfigFactory defaultPostgresConfigFactory;

  private final ConnectedClustersScanner connectedClustersScanner;

  private final ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer;

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public DistributedLogsRequiredResourcesGenerator(
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      DefaultProfileFactory defaultProfileFactory,
      DefaultDistributedLogsPostgresConfigFactory defaultPostgresConfigFactory,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer,
      ConnectedClustersScanner connectedClustersScanner,
      ResourceFinder<Secret> secretFinder) {
    this.configScanner = configScanner;
    this.profileFinder = profileFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.defaultProfileFactory = defaultProfileFactory;
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
    this.clusterFinder = clusterFinder;
    this.discoverer = discoverer;
    this.connectedClustersScanner = connectedClustersScanner;
    this.secretFinder = secretFinder;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDistributedLogs distributedLogs) {
    final String distributedLogsName = distributedLogs.getMetadata().getName();
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));

    final Optional<StackGresProfile> profile = profileFinder.findByNameAndNamespace(
        distributedLogs.getSpec().getSgInstanceProfile(), namespace);
    if (!distributedLogs.getSpec().getSgInstanceProfile()
        .equals(defaultProfileFactory.getDefaultResourceName(distributedLogs))
        && profile.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresProfile.KIND + " " + distributedLogs.getSpec().getSgInstanceProfile()
          + " was not found");
    }

    final Optional<StackGresPostgresConfig> postgresConfig = postgresConfigFinder.findByNameAndNamespace(
        distributedLogs.getSpec().getConfigurations().getSgPostgresConfig(), namespace);
    if (!distributedLogs.getSpec().getConfigurations().getSgPostgresConfig()
        .equals(defaultPostgresConfigFactory.getDefaultResourceName(distributedLogs))
        && postgresConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND + " " + distributedLogs.getSpec().getConfigurations().getSgPostgresConfig()
          + " was not found");
    }

    final Optional<StackGresCluster> cluster = clusterFinder.findByNameAndNamespace(
        distributedLogsName, namespace);

    final @NotNull Optional<Secret> databaseCredentials =
        secretFinder.findByNameAndNamespace(distributedLogsName, namespace)
        .or(() -> secretFinder.findByNameAndNamespace(
            DistributedLogsCredentials.secretName(distributedLogs), namespace));

    StackGresDistributedLogsContext context = ImmutableStackGresDistributedLogsContext.builder()
        .config(config)
        .source(distributedLogs)
        .profile(profile)
        .postgresConfig(postgresConfig)
        .cluster(cluster)
        .addAllConnectedClusters(getConnectedClusters(distributedLogs))
        .databaseCredentials(databaseCredentials)
        .build();

    return discoverer.generateResources(context);
  }

  private List<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs) {
    return connectedClustersScanner.getConnectedClusters(distributedLogs);
  }
}
