/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgshardedcluster.ShardedClusterEventReason;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.context.ClusterPostgresVersionContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.validation.ValidationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ShardedClusterPostgresVersionContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  public static final Map<String, String> BUGGY_PG_VERSIONS =
      ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS;

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  private final EventEmitter<StackGresShardedCluster> eventController;
  private final ShardedClusterCoordinatorPostgresConfigContextAppender clusterCoordinatorPostgresConfigContextAppender;
  private final ShardedClusterShardsPostgresConfigContextAppender clusterShardsPostgresConfigContextAppender;
  private final ShardedClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender;
  private final ShardedClusterExtensionsContextAppender clusterExtensionsContextAppender;
  private final ShardedClusterCoordinatorClusterContextAppender clusterCoordinatorContextAppender;
  private final ShardedClusterShardsClustersContextAppender clusterShardsContextAppender;

  @Inject
  public ShardedClusterPostgresVersionContextAppender(
      EventEmitter<StackGresShardedCluster> eventController,
      ShardedClusterCoordinatorPostgresConfigContextAppender clusterCoordinatorPostgresConfigContextAppender,
      ShardedClusterShardsPostgresConfigContextAppender clusterShardsPostgresConfigContextAppender,
      ShardedClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender,
      ShardedClusterExtensionsContextAppender clusterExtensionsContextAppender,
      ShardedClusterCoordinatorClusterContextAppender clusterCoordinatorContextAppender,
      ShardedClusterShardsClustersContextAppender clusterShardsContextAppender) {
    this(
        eventController,
        clusterCoordinatorPostgresConfigContextAppender,
        clusterShardsPostgresConfigContextAppender,
        clusterRestoreBackupContextAppender,
        clusterExtensionsContextAppender,
        clusterCoordinatorContextAppender,
        clusterShardsContextAppender,
        ValidationUtil.SUPPORTED_POSTGRES_VERSIONS);
  }

  public ShardedClusterPostgresVersionContextAppender(
      EventEmitter<StackGresShardedCluster> eventController,
      ShardedClusterCoordinatorPostgresConfigContextAppender clusterCoordinatorPostgresConfigContextAppender,
      ShardedClusterShardsPostgresConfigContextAppender clusterShardsPostgresConfigContextAppender,
      ShardedClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender,
      ShardedClusterExtensionsContextAppender clusterExtensionsContextAppender,
      ShardedClusterCoordinatorClusterContextAppender clusterCoordinatorContextAppender,
      ShardedClusterShardsClustersContextAppender clusterShardsContextAppender,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.eventController = eventController;
    this.clusterCoordinatorPostgresConfigContextAppender = clusterCoordinatorPostgresConfigContextAppender;
    this.clusterShardsPostgresConfigContextAppender = clusterShardsPostgresConfigContextAppender;
    this.clusterRestoreBackupContextAppender = clusterRestoreBackupContextAppender;
    this.clusterExtensionsContextAppender = clusterExtensionsContextAppender;
    this.clusterCoordinatorContextAppender = clusterCoordinatorContextAppender;
    this.clusterShardsContextAppender = clusterShardsContextAppender;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresShardedClusterStatus());
    }
    Optional<String> previousVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresShardedClusterStatus::getPostgresVersion);
    Optional<String> previousBuildVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresShardedClusterStatus::getBuildVersion);
    String givenVersion = Optional.ofNullable(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(StackGresComponent.LATEST);

    if (!isPostgresVersionSupported(cluster, givenVersion)) {
      throw new IllegalArgumentException(
          "Unsupported postgres version " + givenVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
              .get(StackGresVersion.getStackGresVersion(cluster)))
          .toString(", "));
    }

    String version = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getVersion(givenVersion);
    String buildVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getBuildVersion(givenVersion);

    if (BUGGY_PG_VERSIONS.keySet().contains(version)) {
      throw new IllegalArgumentException(
          "Do not use PostgreSQL " + version + ". "
              + BUGGY_PG_VERSIONS.get(version));
    }

    if (previousVersion
        .filter(Predicate.not(version::equals))
        .isPresent()) {
      String majorVersion = getPostgresFlavorComponent(cluster).get(cluster)
          .getMajorVersion(version);
      long majorVersionIndex = getPostgresFlavorComponent(cluster)
          .get(cluster).streamOrderedMajorVersions()
          .zipWithIndex()
          .filter(t -> t.v1.equals(majorVersion))
          .map(Tuple2::v2)
          .findAny()
          .get();
      String previousMajorVersion = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .getMajorVersion(previousVersion.get());
      long previousMajorVersionIndex = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .streamOrderedMajorVersions()
          .zipWithIndex()
          .filter(t -> t.v1.equals(previousMajorVersion))
          .map(Tuple2::v2)
          .findAny()
          .get();
      if (majorVersionIndex < previousMajorVersionIndex
          && (
              cluster.getStatus().getDbOps() == null
              || cluster.getStatus().getDbOps().getMajorVersionUpgrade() == null)) {
        eventController.sendEvent(
            ShardedClusterEventReason.SHARDED_CLUSTER_MAJOR_UPGRADE,
            "To upgrade to major Postgres version " + majorVersion + ", please create an SGShardedDbOps operation"
                + " with \"op: majorVersionUpgrade\" and set the target postgres version to " + version + ".",
            cluster);
        version = null;
      }
      if (majorVersionIndex > previousMajorVersionIndex) {
        throw new IllegalArgumentException("Can not change the major version " + majorVersion
            + " of Postgres to the previous major version " + previousMajorVersion);
      }
    }

    if (version != null && buildVersion != null) {
      cluster.getStatus().setPostgresVersion(version);
      cluster.getStatus().setBuildVersion(buildVersion);
      clusterCoordinatorPostgresConfigContextAppender.appendContext(cluster, contextBuilder, version);
      clusterShardsPostgresConfigContextAppender.appendContext(cluster, contextBuilder, version);
      clusterRestoreBackupContextAppender.appendContext(cluster, contextBuilder, version);
      clusterExtensionsContextAppender.appendContext(cluster, contextBuilder, version,
          buildVersion, previousVersion, previousBuildVersion);
      clusterCoordinatorContextAppender.appendContext(cluster, contextBuilder);
      clusterShardsContextAppender.appendContext(cluster, contextBuilder);
    }

    if ((version == null && previousVersion.isEmpty())
        || (buildVersion == null && previousBuildVersion.isEmpty())) {
      throw new IllegalArgumentException("Can not determine the Postgres version to use");
    }
  }

  private boolean isPostgresVersionSupported(StackGresShardedCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}
