/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class PostgresConfigValidator implements ClusterValidator {

  private final KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private final Set<String> supportedPostgresVersions;

  @Inject
  public PostgresConfigValidator(
      KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder) {
    this(configFinder, StackGresComponents.getAsArray("postgresql"));
  }

  public PostgresConfigValidator(
      KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder,
      String[] supportedPostgresVersions) {
    this.configFinder = configFinder;
    this.supportedPostgresVersions = new HashSet<String>(
        Arrays.asList(supportedPostgresVersions));
  }

  @Override
  public void validate(StackgresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();

    if (cluster == null) {
      return;
    }

    String givenPgVersion = cluster.getSpec().getPostgresVersion();
    String pgConfig = cluster.getSpec().getPostgresConfig();

    checkIfProvided(givenPgVersion, "pgVersion");
    checkIfProvided(pgConfig, "pgConfig");

    if (!isPostgresVersionSupported(givenPgVersion)) {
      throw new ValidationFailed("Unsupported pgVersion " + givenPgVersion
          + ".  Supported postgres versions are: "
          + String.join(", ", supportedPostgresVersions));
    }

    String givenMajorVersion = StackGresComponents.getMajorVersion(givenPgVersion);
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

        if (!givenPgVersion.equals(oldPgVersion)) {
          throw new ValidationFailed("pgVersion cannot be updated");
        }

        break;
      default:
    }

  }

  private void validateAgainstConfiguration(String givenMajorVersion,
                                            String pgConfig,
                                            String namespace)
      throws ValidationFailed {
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
