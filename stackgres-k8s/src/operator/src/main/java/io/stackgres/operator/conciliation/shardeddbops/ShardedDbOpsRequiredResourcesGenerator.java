/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedDbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedDbOpsRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer;

  @Inject
  public ShardedDbOpsRequiredResourcesGenerator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer) {
    this.clusterFinder = clusterFinder;
    this.profileFinder = profileFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedDbOps config) {
    final ObjectMeta metadata = config.getMetadata();
    final String dbOpsNamespace = metadata.getNamespace();

    final StackGresShardedDbOpsSpec spec = config.getSpec();
    final Optional<StackGresShardedCluster> cluster = clusterFinder
        .findByNameAndNamespace(spec.getSgShardedCluster(), dbOpsNamespace);

    final Optional<StackGresProfile> profile = cluster
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getSgInstanceProfile)
        .flatMap(profileName -> profileFinder
            .findByNameAndNamespace(profileName, dbOpsNamespace));

    StackGresShardedDbOpsContext context = ImmutableStackGresShardedDbOpsContext.builder()
        .source(config)
        .foundShardedCluster(cluster)
        .foundProfile(profile)
        .build();

    return discoverer.generateResources(context);
  }

}
