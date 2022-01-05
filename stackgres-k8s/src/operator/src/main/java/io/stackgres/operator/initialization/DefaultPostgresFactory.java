/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;
import java.util.Properties;

import javax.enterprise.context.Dependent;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import org.jetbrains.annotations.NotNull;

@Dependent
public class DefaultPostgresFactory extends AbstractCustomResourceFactory<StackGresPostgresConfig>
    implements PostgresConfigurationFactory {

  public static final String NAME = "defaultpgconfig";

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
  List<String> getExclusionProperties() {
    return List.copyOf(PostgresBlocklist.getBlocklistParameters());
  }

  @Override
  StackGresPostgresConfig buildResource(String namespace) {

    StackGresPostgresConfigSpec spec = new StackGresPostgresConfigSpec();
    spec.setPostgresVersion(postgresVersion);
    spec.setPostgresqlConf(getDefaultValues());

    StackGresPostgresConfig profile = new StackGresPostgresConfig();
    profile.getMetadata().setName(generateDefaultName());
    profile.getMetadata().setNamespace(namespace);
    profile.setSpec(spec);

    return profile;
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
