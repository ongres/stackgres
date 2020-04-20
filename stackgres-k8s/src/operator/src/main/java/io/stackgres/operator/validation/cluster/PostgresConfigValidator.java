/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PostgresConfigValidator implements ClusterValidator {

  private final CustomResourceFinder<StackGresPostgresConfig> configFinder;

  private final Set<String> supportedPostgresVersions;

  private String errorCrReferencerUri;
  private String errorPostgresMismatchUri;
  private String errorForbiddenUpdateUri;

  @Inject
  public PostgresConfigValidator(
      CustomResourceFinder<StackGresPostgresConfig> configFinder, ConfigContext context) {
    this(configFinder, StackGresComponents.getAllOrderedPostgresVersions().toList());
    errorCrReferencerUri = context.getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);
    errorPostgresMismatchUri = context.getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
    errorForbiddenUpdateUri = context.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  public PostgresConfigValidator(
      CustomResourceFinder<StackGresPostgresConfig> configFinder,
      List<String> supportedPostgresVersions) {
    this.configFinder = configFinder;
    this.supportedPostgresVersions = new HashSet<String>(
        supportedPostgresVersions);
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();

    if (cluster == null) {
      return;
    }

    String givenPgVersion = cluster.getSpec().getPostgresVersion();
    String pgConfig = cluster.getSpec().getConfiguration().getPostgresConfig();

    checkIfProvided(pgConfig, "pgConfig");

    if (givenPgVersion != null && !isPostgresVersionSupported(givenPgVersion)) {
      final String message = "Unsupported pgVersion " + givenPgVersion
          + ".  Supported postgres versions are: "
          + StackGresComponents.getAllOrderedPostgresVersions().toString(", ");
      fail(errorPostgresMismatchUri, message);
    }

    String calculatedPgVersion = StackGresComponents.calculatePostgresVersion(givenPgVersion);
    String givenMajorVersion = StackGresComponents.getPostgresMajorVersion(calculatedPgVersion);
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

        String oldPgVersion = oldCluster.getSpec().getPostgresVersion();

        String oldCalculatedPgVersion = StackGresComponents.calculatePostgresVersion(oldPgVersion);
        if (!calculatedPgVersion.equals(oldCalculatedPgVersion)) {
          fail(errorForbiddenUpdateUri, "pgVersion cannot be updated");
        }

        break;
      default:
    }

  }

  private void validateAgainstConfiguration(String givenMajorVersion,
                                            String pgConfig,
                                            String namespace) throws ValidationFailed {
    Optional<StackGresPostgresConfig> postgresConfigOpt = configFinder
        .findByNameAndNamespace(pgConfig, namespace);

    if (postgresConfigOpt.isPresent()) {

      StackGresPostgresConfig postgresConfig = postgresConfigOpt.get();
      String pgVersion = postgresConfig.getSpec().getPostgresVersion();

      if (!pgVersion.equals(givenMajorVersion)) {
        final String message = "Invalid pgVersion, must be "
            + pgVersion + " to use pgConfig " + pgConfig;
        fail(errorPostgresMismatchUri, message);
      }

    } else {

      final String message = "Invalid pgConfig value " + pgConfig;
      fail(errorCrReferencerUri, message);
    }
  }

  private boolean isPostgresVersionSupported(String version) {
    return supportedPostgresVersions.contains(version);
  }

}
