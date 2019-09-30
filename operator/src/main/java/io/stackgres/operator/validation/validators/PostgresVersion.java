/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.validators;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.services.PostgresConfigFinder;
import io.stackgres.operator.validation.AdmissionReview;

@ApplicationScoped
public class PostgresVersion implements Validator {

  private PostgresConfigFinder configFinder;

  @Inject
  public PostgresVersion(PostgresConfigFinder configFinder) {
    this.configFinder = configFinder;
  }

  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();

    String givenPgVersion = cluster.getSpec().getPostgresVersion();
    String givenMajorVersion = getMajorVersion(givenPgVersion);

    switch (review.getRequest().getOperation()) {
      case CREATE:

        String pgConfig = cluster.getSpec().getPostgresConfig();
        Optional<StackGresPostgresConfig> postgresConfigOpt = configFinder
            .findPostgresConfig(pgConfig);

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
        break;
      case UPDATE:
        StackGresCluster oldCluster = review.getRequest().getOldObject();
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

  public static String getMajorVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

}
