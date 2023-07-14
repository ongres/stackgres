/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfiguration;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDistributedLogs> {

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final ConnectedClustersScanner connectedClustersScanner;

  private final ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer;

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public DistributedLogsRequiredResourcesGenerator(
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer,
      ConnectedClustersScanner connectedClustersScanner,
      ResourceFinder<Secret> secretFinder) {
    this.profileFinder = profileFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.discoverer = discoverer;
    this.connectedClustersScanner = connectedClustersScanner;
    this.secretFinder = secretFinder;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDistributedLogs config) {
    final String distributedLogsName = config.getMetadata().getName();
    final String namespace = config.getMetadata().getNamespace();
    final StackGresDistributedLogsSpec spec = config.getSpec();
    final StackGresDistributedLogsConfiguration distributedLogsConfiguration =
        spec.getConfiguration();

    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(
            distributedLogsConfiguration.getPostgresConfig(), namespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDistributedLogs " + namespace + "." + distributedLogsName
                + " have a non existent SGPostgresConfig "
                + distributedLogsConfiguration.getPostgresConfig()));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(spec.getResourceProfile(), namespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDistributedLogs " + namespace + "." + distributedLogsName + " have a non existent "
                + StackGresProfile.KIND + " " + spec.getResourceProfile()));

    StackGresDistributedLogsContext context = ImmutableStackGresDistributedLogsContext.builder()
        .source(config)
        .postgresConfig(pgConfig)
        .profile(profile)
        .addAllConnectedClusters(getConnectedClusters(config))
        .databaseCredentials(secretFinder.findByNameAndNamespace(distributedLogsName, namespace))
        .build();

    return discoverer.generateResources(context);
  }

  private List<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs) {
    return connectedClustersScanner.getConnectedClusters(distributedLogs);
  }
}
