/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static io.stackgres.operatorframework.resource.ResourceUtil.getServiceAccountFromUsername;
import static io.stackgres.operatorframework.resource.ResourceUtil.isServiceAccountUsername;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_CREATE)
public class PostgresConfigValidator implements ShardedClusterValidator {

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

  private final String errorPostgresMismatchUri;
  private final String errorForbiddenUpdateUri;

  @Inject
  public PostgresConfigValidator() {
    this(ValidationUtil.SUPPORTED_POSTGRES_VERSIONS);
  }

  public PostgresConfigValidator(
      Map<StackGresComponent, Map<StackGresVersion, List<String>>>
          orderedSupportedPostgresVersions) {
    this.supportedPostgresVersions = orderedSupportedPostgresVersions;
    this.errorPostgresMismatchUri = ErrorType.getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
    this.errorForbiddenUpdateUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CLUSTER_UPDATE);
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();

    if (cluster == null) {
      return;
    }

    String givenPgVersion = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(null);
    String coordinatorPgConfig = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getConfigurationsForCoordinator)
        .map(StackGresClusterConfigurations::getSgPostgresConfig)
        .orElse(null);
    String shardsPgConfig = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getShards)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getSgPostgresConfig)
        .orElse(null);

    if (givenPgVersion == null || coordinatorPgConfig == null || shardsPgConfig == null) {
      return;
    }

    if (!isPostgresVersionSupported(cluster, givenPgVersion)) {
      final String message = "Unsupported postgres version " + givenPgVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(supportedPostgresVersions.get(getPostgresFlavorComponent(cluster)))
          .toString(", ");
      fail(errorPostgresMismatchUri, message);
    }

    String givenMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(givenPgVersion);
    String username = review.getRequest().getUserInfo().getUsername();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        if (getPostgresFlavorComponent(cluster) != StackGresComponent.BABELFISH
            && BUGGY_PG_VERSIONS.keySet().contains(givenPgVersion)) {
          fail("Do not use PostgreSQL " + givenPgVersion + ". "
              + BUGGY_PG_VERSIONS.get(givenPgVersion));
        }
        break;
      case UPDATE:
        StackGresShardedCluster oldCluster = review.getRequest().getOldObject();
        if (!Objects.equals(
            getPostgresFlavorComponent(cluster),
            getPostgresFlavorComponent(oldCluster))) {
          fail(errorForbiddenUpdateUri,
              "postgres flavor can not be changed");
        }

        long givenMajorVersionIndex = getPostgresFlavorComponent(cluster)
            .get(cluster).streamOrderedMajorVersions()
            .zipWithIndex()
            .filter(t -> t.v1.equals(givenMajorVersion))
            .map(Tuple2::v2)
            .findAny()
            .get();
        String oldPgVersion = oldCluster.getSpec().getPostgres().getVersion();
        String oldMajorVersion = getPostgresFlavorComponent(oldCluster)
            .get(cluster)
            .getMajorVersion(oldPgVersion);
        long oldMajorVersionIndex = getPostgresFlavorComponent(oldCluster)
            .get(cluster)
            .streamOrderedMajorVersions()
            .zipWithIndex()
            .filter(t -> t.v1.equals(oldMajorVersion))
            .map(Tuple2::v2)
            .findAny()
            .get();

        if (!oldPgVersion.equals(givenPgVersion)
            && !(
                StackGresUtil.isLocked(cluster)
                && username != null
                && isServiceAccountUsername(username)
                && Objects.equals(
                    StackGresUtil.getLockServiceAccount(cluster),
                    getServiceAccountFromUsername(username))
                )) {
          if (givenMajorVersionIndex != oldMajorVersionIndex) {
            fail(errorForbiddenUpdateUri,
                "to upgrade a major Postgres version, please create an SGDbOps operation"
                    + " with \"op: majorVersionUpgrade\" and the target postgres version.");
          } else {
            fail(errorForbiddenUpdateUri,
                "to upgrade a minor Postgres version, please create an SGDbOps operation"
                    + " with \"op: minorVersionUpgrade\" and the target postgres version.");
          }
        }
        break;
      default:
    }
  }

  private boolean isPostgresVersionSupported(StackGresShardedCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}
