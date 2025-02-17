/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
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
public class ShardedDbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedDbOpsRequiredResourcesGenerator.class);

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer;

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions =
      Stream.of(StackGresComponent.POSTGRESQL, StackGresComponent.BABELFISH)
          .collect(ImmutableMap.toImmutableMap(Function.identity(),
              component -> component.getComponentVersions()
                  .entrySet()
                  .stream()
                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                      entry -> entry.getValue().streamOrderedVersions().toList()))));

  @Inject
  public ShardedDbOpsRequiredResourcesGenerator(
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer) {
    this.configScanner = configScanner;
    this.shardedClusterFinder = shardedClusterFinder;
    this.clusterFinder = clusterFinder;
    this.profileFinder = profileFinder;
    this.postgresConfigFinder = postgresConfigFinder;
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

    final String clusterName = dbOps.getSpec().getSgShardedCluster();
    final Optional<StackGresShardedCluster> foundCluster = shardedClusterFinder
        .findByNameAndNamespace(clusterName, dbOpsNamespace);
    final Optional<StackGresCluster> foundCoordinator = clusterFinder
        .findByNameAndNamespace(
            getCoordinatorClusterName(clusterName), dbOpsNamespace);

    final Optional<StackGresProfile> foundProfile = foundCluster
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getSgInstanceProfile)
        .flatMap(profileName -> profileFinder
            .findByNameAndNamespace(profileName, dbOpsNamespace));

    StackGresShardedDbOpsContext context = ImmutableStackGresShardedDbOpsContext.builder()
        .config(config)
        .source(dbOps)
        .foundShardedCluster(foundCluster)
        .foundCoordinator(foundCoordinator)
        .foundProfile(foundProfile)
        .build();

    if (!ShardedDbOpsUtil.isAlreadyCompleted(dbOps)) {
      if (foundCluster.isEmpty()) {
        throw new IllegalArgumentException(
            StackGresShardedCluster.KIND + " " + clusterName + " not found");
      }
      final StackGresShardedCluster cluster = foundCluster.get();
      if (foundProfile.isEmpty()) {
        throw new IllegalArgumentException(
            StackGresProfile.KIND + " " + cluster.getSpec().getCoordinator().getSgInstanceProfile() + " not found");
      }
      if (foundCoordinator.isEmpty()) {
        throw new IllegalArgumentException(
            StackGresCluster.KIND + " " + getCoordinatorClusterName(clusterName) + " not found");
      }

      if (dbOps.getSpec().isOpMajorVersionUpgrade()) {
        Optional<StackGresPostgresConfig> postgresConfig = postgresConfigFinder
            .findByNameAndNamespace(
                dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig(),
                dbOps.getMetadata().getNamespace());
        var foundOwnerReference = Optional.of(cluster.getMetadata())
            .map(ObjectMeta::getOwnerReferences)
            .stream()
            .flatMap(List::stream)
            .filter(ownerReference -> ownerReference.getController() != null
                && ownerReference.getController())
            .findFirst();
        if (foundOwnerReference.isPresent()) {
          OwnerReference ownerReference = foundOwnerReference.get();
          throw new IllegalArgumentException(
              "Can not perform major version upgrade on " + StackGresShardedCluster.KIND + " managed by "
                  + ownerReference.getKind() + " " + ownerReference.getName());
        }
  
        String givenPgVersion = dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();
  
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
            .map(StackGresShardedClusterStatus::getDbOps)
            .map(StackGresShardedClusterDbOpsStatus::getMajorVersionUpgrade)
            .map(StackGresShardedClusterDbOpsMajorVersionUpgradeStatus
                ::getSourcePostgresVersion)
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
              "postgres version must be a newer major version than the current one");
        }

        if (postgresConfig.isPresent()) {
          if (!postgresConfig.get().getSpec().getPostgresVersion().equals(givenMajorVersion)) {
            throw new IllegalArgumentException(
                StackGresPostgresConfig.KIND + " must be for postgres version " + givenMajorVersion
                + " but it was for version " + postgresConfig.get().getSpec().getPostgresVersion());
          }
        } else {
          throw new IllegalArgumentException(
              StackGresPostgresConfig.KIND + " "
                  + dbOps.getSpec().getMajorVersionUpgrade().getSgPostgresConfig() + " not found");
        }
      }
    }

    return discoverer.generateResources(context);
  }

  private boolean isPostgresVersionSupported(StackGresShardedCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}
