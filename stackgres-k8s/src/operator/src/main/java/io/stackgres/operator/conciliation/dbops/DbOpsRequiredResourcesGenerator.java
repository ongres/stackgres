/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(DbOpsRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final RequiredResourceDecorator<StackGresDbOpsContext> decorator;

  @Inject
  public DbOpsRequiredResourcesGenerator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      RequiredResourceDecorator<StackGresDbOpsContext> decorator) {
    this.clusterFinder = clusterFinder;
    this.profileFinder = profileFinder;
    this.decorator = decorator;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDbOps config) {
    final ObjectMeta metadata = config.getMetadata();
    final String dbOpsName = metadata.getName();
    final String dbOpsNamespace = metadata.getNamespace();

    final StackGresDbOpsSpec spec = config.getSpec();
    final StackGresCluster cluster = clusterFinder
        .findByNameAndNamespace(spec.getSgCluster(), dbOpsNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDbOps " + dbOpsNamespace + "." + dbOpsName
                + " have a non existent SGCluster " + spec.getSgCluster()));

    final StackGresProfile profile = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getResourceProfile)
        .map(profileName -> profileFinder
            .findByNameAndNamespace(profileName, dbOpsNamespace)
            .orElseThrow(() -> new IllegalArgumentException(
                "SGDbOps " + dbOpsNamespace + "." + dbOpsName
                    + " target SGCluster " + spec.getSgCluster()
                    + " with a non existent SGInstanceProfile "
                    + profileName)))
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDbOps " + dbOpsNamespace + "." + dbOpsName
                + " target SGCluster " + spec.getSgCluster()
                + " is missing an SGInstanceProfile"));

    if (config.getSpec().isOpMajorVersionUpgrade()
        && config.getSpec().isMajorVersionUpgradeSectionProvided()) {
      Optional.ofNullable(config.getStatus())
          .map(StackGresDbOpsStatus::getMajorVersionUpgrade)
          .map(StackGresDbOpsMajorVersionUpgradeStatus::getSourcePostgresVersion)
          .ifPresent(postgresVersion -> cluster.getSpec().getPostgres()
              .setVersion(postgresVersion));
    }

    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(config)
        .cluster(cluster)
        .profile(profile)
        .build();

    return decorator.decorateResources(context);
  }

}
