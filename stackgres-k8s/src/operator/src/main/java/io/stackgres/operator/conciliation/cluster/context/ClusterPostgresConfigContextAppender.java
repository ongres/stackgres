/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ongres.pgconfig.validator.GucValidator;
import com.ongres.pgconfig.validator.PgParameter;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultClusterPostgresConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterPostgresConfigContextAppender {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory;

  public ClusterPostgresConfigContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory) {
    this.postgresConfigFinder = postgresConfigFinder;
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
  }

  public void appendContext(StackGresCluster cluster, Builder contextBuilder, String version) {
    final Optional<StackGresPostgresConfig> postgresConfig = postgresConfigFinder
        .findByNameAndNamespace(
            cluster.getSpec().getConfigurations().getSgPostgresConfig(),
            cluster.getMetadata().getNamespace());
    if (!cluster.getSpec().getConfigurations().getSgPostgresConfig().equals(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster))
        && postgresConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND + " "
          + cluster.getSpec().getConfigurations().getSgPostgresConfig()
          + " was not found");
    }
    String majorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(version);
    if (postgresConfig.isPresent()) {
      String postgresConfigVersion = postgresConfig.get().getSpec().getPostgresVersion();
      if (!postgresConfigVersion.equals(majorVersion)) {
        throw new IllegalArgumentException(
            "Invalid postgres version " + version + " for " + StackGresPostgresConfig.KIND
            + " " + cluster.getSpec().getConfigurations().getSgPostgresConfig()
            + " that uses version " + postgresConfigVersion);
      }
    }

    validatePostgresConfig(cluster, majorVersion);

    contextBuilder.postgresConfig(postgresConfig);
  }

  private void validatePostgresConfig(StackGresCluster cluster, String majorVersion) {
    // TODO: Update when dependency update is available
    if (majorVersion.equals("18")) {
      return;
    }
    final GucValidator val = GucValidator.forVersion(majorVersion);
    Optional.ofNullable(cluster.getSpec().getConfigurations().getPostgres())
        .map(StackGresPostgresConfigSpec::getPostgresqlConf)
        .map(Map::entrySet)
        .stream()
        .flatMap(Set::stream)
        .forEach(e -> {
          PgParameter parameter = val.parameter(e.getKey(), e.getValue());
          if (!parameter.isValid()) {
            throw new IllegalArgumentException(
                "Postgres config parameter " + parameter.getName()
                + ": " + parameter.getError().orElseThrow()
                + parameter.getHint().map(hint -> " (" + hint + ")").orElse(""));
          }
        });
  }

}
