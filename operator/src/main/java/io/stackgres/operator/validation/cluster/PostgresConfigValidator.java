/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.services.KubernetesCustomResourceFinder;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.ValidationFailed;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PostgresConfigValidator implements ClusterValidator {

  private KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder;


  private Set<String> supportedPostgresVersions = new LinkedHashSet<>();;

  @Inject
  public PostgresConfigValidator(
      KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder,
      @ConfigProperty(name = "stackgres.supported.major.versions") List<String> majorVersions,
      @ConfigProperty(name = "stackgres.supported.minor.versions") List<Integer> minorVersions) {
    this.configFinder = configFinder;

    for (int i = 0; i < majorVersions.size(); i++){

      String majorVersion = majorVersions.get(i);
      Integer latestMinorVersion = minorVersions.get(i);

      for(int j = 0; j <= latestMinorVersion; j++){
        supportedPostgresVersions.add(majorVersion + "." + j);
      }

    }

  }

  @ConfigProperty(name = "stackgres.supported.major.versions")
  private String majorVersions;

  @ConfigProperty(name = "stackgres.latest.minor.versions")
  private String minorVersions;


  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();

    String givenPgVersion = cluster.getSpec().getPostgresVersion();
    String pgConfig = cluster.getSpec().getPostgresConfig();

    checkIfProvided(givenPgVersion, "pg_version");
    checkIfProvided(pgConfig, "pg_config");

    if (!isPostgresVersionSupported(givenPgVersion)){
      throw new ValidationFailed("Unsupported pg_version " + givenPgVersion
          + ".  Supported postgres versions are: "
          + String.join(", ", supportedPostgresVersions));
    }


    String givenMajorVersion = getMajorVersion(givenPgVersion);
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

        String oldPgMajorVersion = getMajorVersion(oldPgVersion);
        if (!givenMajorVersion.equals(oldPgMajorVersion)) {
          throw new ValidationFailed("Invalid pg_version update, only minor version of postgres "
              + "can be updated, current major version: " + oldPgMajorVersion);
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
      String configuredMajorVersion = getMajorVersion(pgVersion);

      if (!configuredMajorVersion.equals(givenMajorVersion)) {
        throw new ValidationFailed("Invalid pg_version, must be "
            + configuredMajorVersion + ".x to use pfConfig " + pgConfig);
      }

    } else {
      throw new ValidationFailed("Invalid pg_config value " + pgConfig);
    }
  }

  private static String getMajorVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  private boolean isPostgresVersionSupported(String version){
    return supportedPostgresVersions.contains(version);
  }

}
