/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ShardedDbOpsClusterMajorVersionUpgradeContextAppender {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions;

  @Inject
  public ShardedDbOpsClusterMajorVersionUpgradeContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder) {
    this(
        postgresConfigFinder,
        Stream.of(StackGresComponent.POSTGRESQL, StackGresComponent.BABELFISH)
        .collect(ImmutableMap.toImmutableMap(Function.identity(),
            component -> component.getComponentVersions()
                .entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                    entry -> entry.getValue().streamOrderedVersions().toList())))));
  }

  public ShardedDbOpsClusterMajorVersionUpgradeContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.postgresConfigFinder = postgresConfigFinder;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  public void appendContext(StackGresShardedDbOps dbOps, StackGresShardedCluster cluster, Builder contextBuilder) {
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
        .map(StackGresShardedClusterStatus::getDbOps)
        .map(StackGresShardedClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(StackGresShardedClusterDbOpsMajorVersionUpgradeStatus::getSourcePostgresVersion)
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
              + givenMajorVersion + " <= " + oldMajorVersion + ")");
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

  private boolean isPostgresVersionSupported(StackGresShardedCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}
