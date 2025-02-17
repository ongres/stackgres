/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmarkStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbench;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSamplingStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(DbOpsRequiredResourcesGenerator.class);

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions =
      Stream.of(StackGresComponent.POSTGRESQL, StackGresComponent.BABELFISH)
      .collect(ImmutableMap.toImmutableMap(Function.identity(),
          component -> component.getComponentVersions()
              .entrySet()
              .stream()
              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                  entry -> entry.getValue().streamOrderedVersions().toList()))));

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  private final ResourceGenerationDiscoverer<StackGresDbOpsContext> discoverer;

  @Inject
  public DbOpsRequiredResourcesGenerator(
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresDbOps> dbOpsFinder,
      ResourceGenerationDiscoverer<StackGresDbOpsContext> discoverer) {
    this.configScanner = configScanner;
    this.clusterFinder = clusterFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.profileFinder = profileFinder;
    this.dbOpsFinder = dbOpsFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDbOps dbOps) {
    final ObjectMeta metadata = dbOps.getMetadata();
    final String dbOpsNamespace = metadata.getNamespace();

    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));

    final String clusterName = dbOps.getSpec().getSgCluster();
    final Optional<StackGresCluster> foundCluster = clusterFinder
        .findByNameAndNamespace(clusterName, dbOpsNamespace);

    final Optional<StackGresProfile> foundProfile = foundCluster
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getSgInstanceProfile)
        .flatMap(profileName -> profileFinder
            .findByNameAndNamespace(profileName, dbOpsNamespace));
    if (!DbOpsUtil.isAlreadyCompleted(dbOps)) {
      if (foundCluster.isEmpty()) {
        throw new IllegalArgumentException(
            StackGresCluster.KIND + " " + clusterName + " was not found");
      }
      final StackGresCluster cluster = foundCluster.get();

      if (foundProfile.isEmpty()) {
        throw new IllegalArgumentException(
            StackGresProfile.KIND + " " + foundCluster.get().getSpec().getSgInstanceProfile() + " was not found");
      }

      if (dbOps.getSpec().isOpMajorVersionUpgrade()) {
        var foundOwnerReference = Optional.of(cluster)
            .map(StackGresCluster::getMetadata)
            .map(ObjectMeta::getOwnerReferences)
            .stream()
            .flatMap(List::stream)
            .filter(ownerReference -> !Objects.equals(
                ownerReference.getKind(),
                HasMetadata.getKind(StackGresDistributedLogs.class)))
            .filter(ownerReference -> ownerReference.getController() != null
                && ownerReference.getController())
            .findFirst();
        if (foundOwnerReference.isPresent()) {
          OwnerReference ownerReference = foundOwnerReference.get();
          throw new IllegalArgumentException(
              "Can not perform major version upgrade on " + StackGresCluster.KIND + " managed by "
                  + ownerReference.getKind() + " " + ownerReference.getName());
        }
    
        final String givenPgVersion = dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();
        if (givenPgVersion != null
            && !isPostgresVersionSupported(cluster, givenPgVersion)) {
          final String message = "Unsupported postgres version " + givenPgVersion
              + ".  Supported postgres versions are: "
              + Seq.seq(supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
                  .get(StackGresVersion.getStackGresVersion(cluster))).toString(", ");
          throw new IllegalArgumentException(message);
        }
    
        String givenMajorVersion = getPostgresFlavorComponent(cluster)
            .get(cluster).getMajorVersion(givenPgVersion);
        long givenMajorVersionIndex = getPostgresFlavorComponent(cluster)
            .get(cluster)
            .streamOrderedMajorVersions()
            .zipWithIndex()
            .filter(t -> t.v1.equals(givenMajorVersion))
            .map(Tuple2::v2)
            .findAny()
            .orElseThrow();
        String oldPgVersion = Optional.ofNullable(cluster.getStatus())
            .map(StackGresClusterStatus::getDbOps)
            .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
            .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getSourcePostgresVersion)
            .orElse(cluster.getSpec().getPostgres().getVersion());
        String oldMajorVersion = getPostgresFlavorComponent(cluster)
            .get(cluster)
            .getMajorVersion(oldPgVersion);
        long oldMajorVersionIndex = getPostgresFlavorComponent(cluster)
            .get(cluster)
            .streamOrderedMajorVersions()
            .zipWithIndex()
            .filter(t -> t.v1.equals(oldMajorVersion))
            .map(Tuple2::v2)
            .findAny()
            .orElseThrow();
    
        if (givenMajorVersionIndex >= oldMajorVersionIndex) {
          throw new IllegalArgumentException(
              "postgres version must be a newer major version than the current one ("
                  + givenMajorVersion + " < " + oldMajorVersion + ")");
        }
  
        Optional<StackGresPostgresConfig> postgresConfig = postgresConfigFinder
            .findByNameAndNamespace(
                dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig(),
                dbOps.getMetadata().getNamespace());
        if (postgresConfig.isPresent()) {
          if (!postgresConfig.get().getSpec().getPostgresVersion().equals(givenMajorVersion)) {
            throw new IllegalArgumentException(
                StackGresPostgresConfig.KIND + " must be for postgres version "
                    + givenMajorVersion + " but was for version "
                    + postgresConfig.get().getSpec().getPostgresVersion());
          }
        } else {
          throw new IllegalArgumentException(
              StackGresPostgresConfig.KIND + " "
                  + dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig() + " not found");
        }
      }
    }

    final Optional<StackGresDbOpsSamplingStatus> samplingStatus = Optional.of(dbOps.getSpec())
        .map(StackGresDbOpsSpec::getBenchmark)
        .map(StackGresDbOpsBenchmark::getPgbench)
        .map(StackGresDbOpsPgbench::getSamplingSgDbOps)
        .map(samplingDbOpsName -> dbOpsFinder
            .findByNameAndNamespace(samplingDbOpsName, dbOpsNamespace)
            .map(StackGresDbOps::getStatus)
            .map(StackGresDbOpsStatus::getBenchmark)
            .map(StackGresDbOpsBenchmarkStatus::getSampling)
            .orElseThrow(() -> new IllegalArgumentException(
                StackGresDbOps.KIND + " " + samplingDbOpsName
                + " was not found or has no has no sampling status")));

    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundCluster(foundCluster)
        .foundProfile(foundProfile)
        .samplingStatus(samplingStatus)
        .build();

    return discoverer.generateResources(context);
  }

  private boolean isPostgresVersionSupported(StackGresCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}
