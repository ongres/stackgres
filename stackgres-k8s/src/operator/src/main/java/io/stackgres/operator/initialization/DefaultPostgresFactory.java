/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.patroni.factory.parameters.Blacklist;

@ApplicationScoped
public class DefaultPostgresFactory extends AbstractCustomResourceFactory<StackGresPostgresConfig> {

  public static final String NAME = "defaultpgconfig";
  public static final String POSTGRES_DEFAULT_VALUES = "postgresql-default-values.properties";

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
    spec.setPgVersion(StackGresComponents.getPostgresMajorVersion(
        StackGresComponents.calculatePostgresVersion(StackGresComponents.LATEST)));
    spec.setPostgresqlConf(getDefaultValues());

    StackGresPostgresConfig profile = new StackGresPostgresConfig();
    profile.setApiVersion(StackGresPostgresConfigDefinition.APIVERSION);
    profile.setKind(StackGresPostgresConfigDefinition.KIND);
    profile.getMetadata().setName(NAME);
    profile.getMetadata().setNamespace(namespace);
    profile.setSpec(spec);

    return profile;
  }

}
