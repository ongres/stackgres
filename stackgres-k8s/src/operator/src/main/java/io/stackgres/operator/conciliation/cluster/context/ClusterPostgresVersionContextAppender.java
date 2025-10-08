/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.validation.ValidationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterPostgresVersionContextAppender
    extends ContextAppender<StackGresCluster, Builder> {

  private static final String PG_14_CREATE_CONCURRENT_INDEX_BUG =
      "Please, use PostgreSQL 14.4 since it fixes an issue"
          + " with CREATE INDEX CONCURRENTLY and REINDEX CONCURRENTLY that"
          + " could cause silent data corruption of indexes. For more info"
          + " see https://www.postgresql.org/about/news/postgresql-144-released-2470/.";
  public static final Map<String, String> BUGGY_PG_VERSIONS = Map.ofEntries(
      Map.entry("14.0", PG_14_CREATE_CONCURRENT_INDEX_BUG),
      Map.entry("14.1", PG_14_CREATE_CONCURRENT_INDEX_BUG),
      Map.entry("14.2", PG_14_CREATE_CONCURRENT_INDEX_BUG),
      Map.entry("14.3", PG_14_CREATE_CONCURRENT_INDEX_BUG)
      );

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  private final EventEmitter<StackGresCluster> eventController;
  private final ClusterPostgresConfigContextAppender clusterPostgresConfigContextAppender;
  private final ClusterDefaultBackupPathContextAppender clusterDefaultBackupPathContextAppender;
  private final ClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender;
  private final ClusterObjectStorageContextAppender clusterObjectStorageContextAppender;
  private final ClusterExtensionsContextAppender clusterExtensionsContextAppender;

  @Inject
  public ClusterPostgresVersionContextAppender(
      EventEmitter<StackGresCluster> eventController,
      ClusterPostgresConfigContextAppender clusterPostgresConfigContextAppender,
      ClusterDefaultBackupPathContextAppender clusterDefaultBackupPathContextAppender,
      ClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender,
      ClusterObjectStorageContextAppender clusterObjectStorageContextAppender,
      ClusterExtensionsContextAppender clusterExtensionsContextAppender) {
    this(
        eventController,
        clusterPostgresConfigContextAppender,
        clusterDefaultBackupPathContextAppender,
        clusterRestoreBackupContextAppender,
        clusterObjectStorageContextAppender,
        clusterExtensionsContextAppender,
        ValidationUtil.SUPPORTED_POSTGRES_VERSIONS);
  }

  public ClusterPostgresVersionContextAppender(
      EventEmitter<StackGresCluster> eventController,
      ClusterPostgresConfigContextAppender clusterPostgresConfigContextAppender,
      ClusterDefaultBackupPathContextAppender clusterDefaultBackupPathContextAppender,
      ClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender,
      ClusterObjectStorageContextAppender clusterObjectStorageContextAppender,
      ClusterExtensionsContextAppender clusterExtensionsContextAppender,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.eventController = eventController;
    this.clusterPostgresConfigContextAppender = clusterPostgresConfigContextAppender;
    this.clusterDefaultBackupPathContextAppender = clusterDefaultBackupPathContextAppender;
    this.clusterRestoreBackupContextAppender = clusterRestoreBackupContextAppender;
    this.clusterObjectStorageContextAppender = clusterObjectStorageContextAppender;
    this.clusterExtensionsContextAppender = clusterExtensionsContextAppender;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    Optional<String> previousVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPostgresVersion);
    Optional<String> previousBuildVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getBuildVersion);
    boolean isRolloutAllowed = ClusterRolloutUtil.isRolloutAllowed(cluster);
    if (isRolloutAllowed
        && (
            cluster.getMetadata().getAnnotations() == null
            || !Objects.equals(
                cluster.getMetadata().getAnnotations().get(StackGresContext.VERSION_KEY),
                StackGresProperty.OPERATOR_VERSION.getString())
        )) {
      cluster.getMetadata().setAnnotations(
          Seq.seq(
              Optional.ofNullable(cluster.getMetadata().getAnnotations())
              .map(Map::entrySet)
              .stream()
              .flatMap(Set::stream)
              .filter(label -> !StackGresContext.VERSION_KEY.equals(label.getKey())))
          .append(Map.entry(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString()))
          .toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    String postgresVersion = previousVersion.filter(version -> !isRolloutAllowed)
        .orElseGet(() -> Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getVersion)
            .orElse(StackGresComponent.LATEST));

    if (!isPostgresVersionSupported(cluster, postgresVersion)) {
      throw new IllegalArgumentException(
          "Unsupported postgres version " + postgresVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
              .get(StackGresVersion.getStackGresVersion(cluster)))
          .toString(", "));
    }

    String version = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getVersion(postgresVersion);
    String buildVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getBuildVersion(postgresVersion);

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
            ClusterEventReason.CLUSTER_MAJOR_UPGRADE,
            "To upgrade to major Postgres version " + majorVersion + ", please create an SGDbOps operation"
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
      clusterPostgresConfigContextAppender.appendContext(cluster, contextBuilder, version);
      clusterDefaultBackupPathContextAppender.appendContext(cluster, contextBuilder, version);
      clusterRestoreBackupContextAppender.appendContext(cluster, contextBuilder, version);
      clusterObjectStorageContextAppender.appendContext(cluster, contextBuilder, version);
      clusterExtensionsContextAppender.appendContext(cluster, contextBuilder, version,
          buildVersion, previousVersion, previousBuildVersion);
    }

    if ((version == null && previousVersion.isEmpty())
        || (buildVersion == null && previousBuildVersion.isEmpty())) {
      throw new IllegalArgumentException("Can not determine the Postgres version to use");
    }
  }

  private boolean isPostgresVersionSupported(StackGresCluster cluster, String version) {
    if (version.contains(".")) {
      return supportedPostgresVersions
        .get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster)).contains(version);
    }
    return getPostgresFlavorComponent(cluster)
        .get(StackGresVersion.getStackGresVersion(cluster))
        .filter(component -> component.findVersion(version).isPresent())
        .isPresent();
  }

}
