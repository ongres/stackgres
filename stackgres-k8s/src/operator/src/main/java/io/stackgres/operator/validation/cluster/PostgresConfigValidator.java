/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@ApplicationScoped
public class PostgresConfigValidator implements ClusterValidator {

  private final KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private final Set<String> supportedPostgresVersions;

  @Inject
  public PostgresConfigValidator(
      KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder) {
    this(configFinder, StackGresComponents.getAllOrderedPostgresVersions().toList());
  }

  public PostgresConfigValidator(
      KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder,
      List<String> supportedPostgresVersions) {
    this.configFinder = configFinder;
    this.supportedPostgresVersions = new HashSet<String>(
        supportedPostgresVersions);
  }

  @Override
  public void validate(StackgresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();

    if (cluster == null) {
      return;
    }

    String givenPgVersion = cluster.getSpec().getPostgresVersion();
    String pgConfig = cluster.getSpec().getPostgresConfig();

    checkIfProvided(pgConfig, "pgConfig");

    if (givenPgVersion != null && !isPostgresVersionSupported(givenPgVersion)) {
      throw new ValidationFailed("Unsupported pgVersion " + givenPgVersion
          + ".  Supported postgres versions are: "
          + StackGresComponents.getAllOrderedPostgresVersions().toString(", "));
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

        String oldPgConfig = oldCluster.getSpec().getPostgresConfig();
        if (!oldPgConfig.equals(pgConfig)) {
          validateAgainstConfiguration(givenMajorVersion, pgConfig, namespace);
        }

        String oldPgVersion = oldCluster.getSpec().getPostgresVersion();

        String oldCalculatedPgVersion = StackGresComponents.calculatePostgresVersion(oldPgVersion);
        if (!calculatedPgVersion.equals(oldCalculatedPgVersion)) {
          throw new ValidationFailed("pgVersion cannot be updated");
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
      String pgVersion = postgresConfig.getSpec().getPgVersion();

      if (!pgVersion.equals(givenMajorVersion)) {
        throw new ValidationFailed("Invalid pgVersion, must be "
            + pgVersion + " to use pfConfig " + pgConfig);
      }

    } else {
      throw new ValidationFailed("Invalid pgConfig value " + pgConfig);
    }
  }

  private boolean isPostgresVersionSupported(String version) {
    return supportedPostgresVersions.contains(version);
  }

}
