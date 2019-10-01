/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.services.KubernetesResourceFinder;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.ValidationFailed;

@ApplicationScoped
public class PostgresVersion implements ClusterValidator {

  private KubernetesResourceFinder<StackGresPostgresConfig> configFinder;

  @Inject
  public PostgresVersion(KubernetesResourceFinder<StackGresPostgresConfig> configFinder) {
    this.configFinder = configFinder;
  }

  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();

    String givenPgVersion = cluster.getSpec().getPostgresVersion();
    String givenMajorVersion = getMajorVersion(givenPgVersion);
    String pgConfig = cluster.getSpec().getPostgresConfig();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        validateAgainstConfiguration(givenMajorVersion, pgConfig);
        break;
      case UPDATE:

        StackGresCluster oldCluster = review.getRequest().getOldObject();

        String oldPgConfig = oldCluster.getSpec().getPostgresConfig();
        if (!oldPgConfig.equals(pgConfig)) {
          validateAgainstConfiguration(givenMajorVersion, pgConfig);
        }

        String oldPgVersion = oldCluster.getSpec().getPostgresVersion();

        String oldPgMajorVersion = getMajorVersion(oldPgVersion);
        if (!givenMajorVersion.equals(oldPgMajorVersion)) {
          throw new ValidationFailed("Invalid pg_version update, only minor version of postgres "
              + "can be updated, current major version: " + oldPgMajorVersion);
        }
        break;
      default:
    }

  }

  private void validateAgainstConfiguration(String givenMajorVersion, String pgConfig)
      throws ValidationFailed {
    Optional<StackGresPostgresConfig> postgresConfigOpt = configFinder
        .findByName(pgConfig);

    if (postgresConfigOpt.isPresent()) {

      StackGresPostgresConfig postgresConfig = postgresConfigOpt.get();
      String pgVersion = postgresConfig.getSpec().getPgVersion();
      String configuredMajorVersion = getMajorVersion(pgVersion);

      if (!configuredMajorVersion.equals(givenMajorVersion)) {
        throw new ValidationFailed("Invalid pg_version, must be "
            + configuredMajorVersion + ".x to use pfConfig " + pgConfig);
      }

    } else {
      throw new ValidationFailed("Invalid pg_config value " + pgConfig);
    }
  }

  public static String getMajorVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

}
