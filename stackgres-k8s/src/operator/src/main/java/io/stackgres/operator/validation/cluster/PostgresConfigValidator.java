/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static io.stackgres.operatorframework.resource.ResourceUtil.getServiceAccountFromUsername;
import static io.stackgres.operatorframework.resource.ResourceUtil.isServiceAccountUsername;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PostgresConfigValidator implements ClusterValidator {

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

  private final CustomResourceFinder<StackGresPostgresConfig> configFinder;

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  private final String errorCrReferencerUri;
  private final String errorPostgresMismatchUri;
  private final String errorForbiddenUpdateUri;
  private final int timeout;

  @Inject
  public PostgresConfigValidator(
      CustomResourceFinder<StackGresPostgresConfig> configFinder,
      OperatorPropertyContext operatorPropertyContext) {
    this(configFinder, ValidationUtil.SUPPORTED_POSTGRES_VERSIONS,
        operatorPropertyContext);
  }

  public PostgresConfigValidator(
      CustomResourceFinder<StackGresPostgresConfig> configFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>>
          orderedSupportedPostgresVersions,
      OperatorPropertyContext operatorPropertyContext) {
    this.configFinder = configFinder;
    this.supportedPostgresVersions = orderedSupportedPostgresVersions;
    this.errorCrReferencerUri = ErrorType.getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);
    this.errorPostgresMismatchUri = ErrorType.getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
    this.errorForbiddenUpdateUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
    this.timeout = operatorPropertyContext.getInt(OperatorProperty.LOCK_TIMEOUT);
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();

    if (cluster == null) {
      return;
    }

    String givenPgVersion = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(null);
    String pgConfig = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getPostgresConfig)
        .orElse(null);

    checkIfProvided(givenPgVersion, "postgres version");
    checkIfProvided(pgConfig, "sgPostgresConfig");

    if (givenPgVersion != null && !isPostgresVersionSupported(cluster, givenPgVersion)) {
      final String message = "Unsupported postgres version " + givenPgVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(supportedPostgresVersions.get(getPostgresFlavorComponent(cluster)))
          .toString(", ");
      fail(errorPostgresMismatchUri, message);
    }

    String givenMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(givenPgVersion);
    String namespace = cluster.getMetadata().getNamespace();
    String username = review.getRequest().getUserInfo().getUsername();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        if (BUGGY_PG_VERSIONS.keySet().contains(givenPgVersion)) {
          fail(errorForbiddenUpdateUri, "Do not use PostgreSQL " + givenPgVersion + ". "
              + BUGGY_PG_VERSIONS.get(givenPgVersion));
        }
        validateAgainstConfiguration(givenMajorVersion, pgConfig, namespace);
        break;
      case UPDATE:
        StackGresCluster oldCluster = review.getRequest().getOldObject();
        if (!Objects.equals(
            getPostgresFlavorComponent(cluster),
            getPostgresFlavorComponent(oldCluster))) {
          fail(errorForbiddenUpdateUri,
              "postgres flavor can not be changed");
        }

        String oldPgConfig = oldCluster.getSpec().getConfiguration().getPostgresConfig();
        if (!oldPgConfig.equals(pgConfig)) {
          validateAgainstConfiguration(givenMajorVersion, pgConfig, namespace);
        }

        long givenMajorVersionIndex = getPostgresFlavorComponent(cluster)
            .get(cluster).getOrderedMajorVersions()
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
            .getOrderedMajorVersions()
            .zipWithIndex()
            .filter(t -> t.v1.equals(oldMajorVersion))
            .map(Tuple2::v2)
            .findAny()
            .get();

        if (givenMajorVersionIndex > oldMajorVersionIndex) {
          fail(errorForbiddenUpdateUri,
              "postgres version can not be changed to a previous major version");
        }

        if (!oldPgVersion.equals(givenPgVersion)
            && !(
                StackGresUtil.isLocked(cluster, timeout)
                && username != null
                && isServiceAccountUsername(username)
                && Objects.equals(
                    StackGresUtil.getLockServiceAccount(cluster),
                    getServiceAccountFromUsername(username))
                )) {
          if (givenMajorVersionIndex < oldMajorVersionIndex) {
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

  private void validateAgainstConfiguration(String givenMajorVersion,
      String pgConfig, String namespace) throws ValidationFailed {
    Optional<StackGresPostgresConfig> postgresConfigOpt = configFinder
        .findByNameAndNamespace(pgConfig, namespace);

    if (postgresConfigOpt.isPresent()) {

      StackGresPostgresConfig postgresConfig = postgresConfigOpt.get();
      String pgVersion = postgresConfig.getSpec().getPostgresVersion();

      if (!pgVersion.equals(givenMajorVersion)) {
        final String message = "Invalid postgres version, must be "
            + pgVersion + " to use sgPostgresConfig " + pgConfig;
        fail(errorPostgresMismatchUri, message);
      }

    } else {

      final String message = "Invalid sgPostgresConfig value " + pgConfig;
      fail(errorCrReferencerUri, message);
    }
  }

  private boolean isPostgresVersionSupported(StackGresCluster cluster, String version) {
    return supportedPostgresVersions.get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(version);
  }

}
