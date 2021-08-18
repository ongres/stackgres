/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PostgresConfigValidator implements ClusterValidator {

  private final CustomResourceFinder<StackGresPostgresConfig> configFinder;

  private final List<String> supportedPostgresVersions;

  private final String errorCrReferencerUri;
  private final String errorPostgresMismatchUri;
  private final String errorForbiddenUpdateUri;

  @Inject
  public PostgresConfigValidator(
      CustomResourceFinder<StackGresPostgresConfig> configFinder) {
    this(configFinder, StackGresComponent.POSTGRESQL.getOrderedVersions().toList());
  }

  public PostgresConfigValidator(
      CustomResourceFinder<StackGresPostgresConfig> configFinder,
      List<String> orderedSupportedPostgresVersions) {
    this.configFinder = configFinder;
    this.supportedPostgresVersions = new ArrayList<String>(orderedSupportedPostgresVersions);
    this.errorCrReferencerUri = ErrorType.getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);
    this.errorPostgresMismatchUri = ErrorType.getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
    this.errorForbiddenUpdateUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();

    if (cluster == null) {
      return;
    }

    String givenPgVersion = cluster.getSpec().getPostgres().getVersion();
    String pgConfig = cluster.getSpec().getConfiguration().getPostgresConfig();

    checkIfProvided(givenPgVersion, "postgres version");
    checkIfProvided(pgConfig, "sgPostgresConfig");

    if (givenPgVersion != null && !isPostgresVersionSupported(givenPgVersion)) {
      final String message = "Unsupported postgres version " + givenPgVersion
          + ".  Supported postgres versions are: "
          + StackGresComponent.POSTGRESQL.getOrderedVersions().toString(", ");
      fail(errorPostgresMismatchUri, message);
    }

    String givenMajorVersion = StackGresComponent.POSTGRESQL.findMajorVersion(givenPgVersion);
    String namespace = cluster.getMetadata().getNamespace();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        validateAgainstConfiguration(givenMajorVersion, pgConfig, namespace);
        break;
      case UPDATE:

        StackGresCluster oldCluster = review.getRequest().getOldObject();

        String oldPgConfig = oldCluster.getSpec().getConfiguration().getPostgresConfig();
        if (!oldPgConfig.equals(pgConfig)) {
          validateAgainstConfiguration(givenMajorVersion, pgConfig, namespace);
        }

        long givenMajorVersionIndex = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
            .zipWithIndex()
            .filter(t -> t.v1.equals(givenMajorVersion))
            .map(Tuple2::v2)
            .findAny()
            .get();
        String oldPgVersion = oldCluster.getSpec().getPostgres().getVersion();
        String oldMajorVersion = StackGresComponent.POSTGRESQL.findMajorVersion(oldPgVersion);
        long oldMajorVersionIndex = StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
            .zipWithIndex()
            .filter(t -> t.v1.equals(oldMajorVersion))
            .map(Tuple2::v2)
            .findAny()
            .get();

        if (givenMajorVersionIndex > oldMajorVersionIndex) {
          fail(errorForbiddenUpdateUri,
              "postgres version can not be changed to a previous major version");
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

  private boolean isPostgresVersionSupported(String version) {
    return supportedPostgresVersions.contains(version);
  }

}
