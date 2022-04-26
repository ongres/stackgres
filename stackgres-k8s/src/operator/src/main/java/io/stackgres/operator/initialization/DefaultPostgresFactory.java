/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.Dependent;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import org.jetbrains.annotations.NotNull;

@Dependent
public class DefaultPostgresFactory extends AbstractCustomResourceFactory<StackGresPostgresConfig>
    implements PostgresConfigurationFactory {

  private @NotNull String postgresVersion;

  public DefaultPostgresFactory() {
    this.postgresVersion = StackGresComponent.POSTGRESQL.getLatest().findMajorVersion(
        StackGresComponent.LATEST);
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return PostgresDefaultValues.getProperties(postgresVersion);
  }

  @Override
  StackGresPostgresConfig buildResource(String namespace) {
    StackGresPostgresConfigSpec spec = new StackGresPostgresConfigSpec();
    spec.setPostgresVersion(postgresVersion);
    spec.setPostgresqlConf(Map.of());
    StackGresPostgresConfigStatus status = new StackGresPostgresConfigStatus();
    status.setDefaultParameters(getDefaultValues());

    StackGresPostgresConfig pgConfig = new StackGresPostgresConfig();
    pgConfig.getMetadata().setName(generateDefaultName());
    pgConfig.getMetadata().setNamespace(namespace);
    pgConfig.setSpec(spec);
    pgConfig.setStatus(status);

    return pgConfig;
  }

  @Override
  public String generateDefaultName() {
    return getDefaultPrefix()
        + System.currentTimeMillis();
  }

  @Override
  public String getDefaultPrefix() {
    return "postgres-"
        + postgresVersion
        + "-"
        + DEFAULT_RESOURCE_NAME_PREFIX;
  }

  @Override
  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(@NotNull String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }
}
