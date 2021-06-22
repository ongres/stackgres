/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import javax.enterprise.context.Dependent;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.Blocklist;

@Dependent
public class DefaultPostgresFactory extends AbstractCustomResourceFactory<StackGresPostgresConfig>
    implements PostgresConfigurationFactory {

  public static final String NAME = "defaultpgconfig";
  public static final String POSTGRES_DEFAULT_VALUES = "postgresql-default-values.properties";

  private String postgresVersion;

  public DefaultPostgresFactory() {
    this.postgresVersion = StackGresComponent.POSTGRESQL.findMajorVersion(
        StackGresComponent.LATEST);
  }

  @Override
  String getDefaultPropertiesFile() {
    return POSTGRES_DEFAULT_VALUES;
  }

  @Override
  List<String> getExclusionProperties() {
    return Blocklist.getBlocklistParameters();
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

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }
}
