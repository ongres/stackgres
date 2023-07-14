/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(DbOpsRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final ResourceGenerationDiscoverer<StackGresDbOpsContext> discoverer;

  @Inject
  public DbOpsRequiredResourcesGenerator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      ResourceGenerationDiscoverer<StackGresDbOpsContext> discoverer) {
    this.clusterFinder = clusterFinder;
    this.profileFinder = profileFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDbOps config) {
    final ObjectMeta metadata = config.getMetadata();
    final String dbOpsNamespace = metadata.getNamespace();

    final StackGresDbOpsSpec spec = config.getSpec();
    final Optional<StackGresCluster> cluster = clusterFinder
        .findByNameAndNamespace(spec.getSgCluster(), dbOpsNamespace);

    final Optional<StackGresProfile> profile = cluster
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getResourceProfile)
        .flatMap(profileName -> profileFinder
            .findByNameAndNamespace(profileName, dbOpsNamespace));

    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(config)
        .foundCluster(cluster)
        .foundProfile(profile)
        .build();

    return discoverer.generateResources(context);
  }

}
