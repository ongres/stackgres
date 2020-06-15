/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import javax.enterprise.context.Dependent;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.patroni.factory.parameters.Blacklist;

@Dependent
public class DefaultPostgresFactory extends AbstractCustomResourceFactory<StackGresPostgresConfig>
    implements PostgresConfigurationFactory {

  public static final String NAME = "defaultpgconfig";
  public static final String POSTGRES_DEFAULT_VALUES = "postgresql-default-values.properties";

  private String postgresVersion;

  public DefaultPostgresFactory() {
    this.postgresVersion = StackGresComponents.getPostgresMajorVersion(
        StackGresComponents.calculatePostgresVersion(StackGresComponents.LATEST));
  }

  @Override
  String getDefaultPropertiesFile() {
    return POSTGRES_DEFAULT_VALUES;
  }

  @Override
  List<String> getExclusionProperties() {
    return Blacklist.getBlacklistParameters();
  }

  @Override
  StackGresPostgresConfig buildResource(String namespace) {

    StackGresPostgresConfigSpec spec = new StackGresPostgresConfigSpec();
    spec.setPostgresVersion(postgresVersion);
    spec.setPostgresqlConf(getDefaultValues());

    StackGresPostgresConfig profile = new StackGresPostgresConfig();
    profile.setApiVersion(StackGresPostgresConfigDefinition.APIVERSION);
    profile.setKind(StackGresPostgresConfigDefinition.KIND);
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
