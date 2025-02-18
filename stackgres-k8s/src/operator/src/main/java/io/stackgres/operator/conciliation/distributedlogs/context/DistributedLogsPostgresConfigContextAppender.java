/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import java.util.Optional;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext.Builder;
import io.stackgres.operator.initialization.DefaultDistributedLogsPostgresConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DistributedLogsPostgresConfigContextAppender
    extends ContextAppender<StackGresDistributedLogs, Builder> {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final DefaultDistributedLogsPostgresConfigFactory defaultPostgresConfigFactory;

  public DistributedLogsPostgresConfigContextAppender(
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      DefaultDistributedLogsPostgresConfigFactory defaultPostgresConfigFactory) {
    this.postgresConfigFinder = postgresConfigFinder;
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
  }

  @Override
  public void appendContext(StackGresDistributedLogs cluster, Builder contextBuilder) {
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
    contextBuilder.postgresConfig(postgresConfig);
  }

}
