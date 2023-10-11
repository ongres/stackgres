/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfigurations;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;

@ApplicationScoped
public class DistributedLogsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDistributedLogs> {

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final ConnectedClustersScanner connectedClustersScanner;

  private final ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer;

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public DistributedLogsRequiredResourcesGenerator(
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer,
      ConnectedClustersScanner connectedClustersScanner,
      ResourceFinder<Secret> secretFinder) {
    this.configScanner = configScanner;
    this.profileFinder = profileFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.discoverer = discoverer;
    this.connectedClustersScanner = connectedClustersScanner;
    this.secretFinder = secretFinder;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDistributedLogs distributedLogs) {
    final String distributedLogsName = distributedLogs.getMetadata().getName();
    final String namespace = distributedLogs.getMetadata().getNamespace();
    final StackGresDistributedLogsSpec spec = distributedLogs.getSpec();
    final StackGresDistributedLogsConfigurations distributedLogsConfiguration =
        spec.getConfigurations();

    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));

    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(
            distributedLogsConfiguration.getSgPostgresConfig(), namespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDistributedLogs " + namespace + "." + distributedLogsName
                + " have a non existent SGPostgresConfig "
                + distributedLogsConfiguration.getSgPostgresConfig()));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(spec.getSgInstanceProfile(), namespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDistributedLogs " + namespace + "." + distributedLogsName + " have a non existent "
                + StackGresProfile.KIND + " " + spec.getSgInstanceProfile()));

    StackGresDistributedLogsContext context = ImmutableStackGresDistributedLogsContext.builder()
        .config(config)
        .source(distributedLogs)
        .postgresConfig(pgConfig)
        .profile(profile)
        .addAllConnectedClusters(getConnectedClusters(distributedLogs))
        .databaseCredentials(secretFinder.findByNameAndNamespace(distributedLogsName, namespace))
        .build();

    return discoverer.generateResources(context);
  }

  private List<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs) {
    return connectedClustersScanner.getConnectedClusters(distributedLogs);
  }
}
