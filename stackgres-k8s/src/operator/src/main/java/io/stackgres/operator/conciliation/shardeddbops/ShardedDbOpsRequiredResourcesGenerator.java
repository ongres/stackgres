/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedDbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedDbOpsRequiredResourcesGenerator.class);

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer;

  @Inject
  public ShardedDbOpsRequiredResourcesGenerator(
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer) {
    this.configScanner = configScanner;
    this.shardedClusterFinder = shardedClusterFinder;
    this.clusterFinder = clusterFinder;
    this.profileFinder = profileFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedDbOps dbOps) {
    final ObjectMeta metadata = dbOps.getMetadata();
    final String dbOpsNamespace = metadata.getNamespace();

    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));

    final StackGresShardedDbOpsSpec spec = dbOps.getSpec();
    final Optional<StackGresShardedCluster> cluster = shardedClusterFinder
        .findByNameAndNamespace(spec.getSgShardedCluster(), dbOpsNamespace);
    final Optional<StackGresCluster> coordinator = clusterFinder
        .findByNameAndNamespace(
            getCoordinatorClusterName(spec.getSgShardedCluster()), dbOpsNamespace);

    final Optional<StackGresProfile> profile = cluster
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getSgInstanceProfile)
        .flatMap(profileName -> profileFinder
            .findByNameAndNamespace(profileName, dbOpsNamespace));

    StackGresShardedDbOpsContext context = ImmutableStackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(cluster)
        .foundCoordinator(coordinator)
        .foundProfile(profile)
        .build();

    return discoverer.generateResources(context);
  }

}
