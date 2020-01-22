/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.patroni.parameters.Blacklist;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultPostgresFactory extends AbstractCustomResourceFactory<StackGresPostgresConfig> {

  public static final String NAME = "defaultpgconfig";
  public static final String POSTGRES_DEFAULT_VALUES = "postgresql-default-values.properties";

  private final String defaultPostgresVersion;

  @Inject
  public DefaultPostgresFactory(
      @ConfigProperty(name = "stackgres.supported.major.versions") List<String> majorVersions) {

    defaultPostgresVersion = Seq.of(StackGresComponents.getAsArray("postgresql"))
        .map(StackGresComponents::getMajorVersion)
        .map(Integer::parseInt)
        .sorted(Comparator.reverseOrder())
        .map(Object::toString)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("major supported version must configured"));
  }

  @Override
  String getDefaultPropertiesFile() {
    return POSTGRES_DEFAULT_VALUES;
  }

  public String getDefaultPostgresVersion() {
    return defaultPostgresVersion;
  }

  @Override
  List<String> getExclusionProperties() {
    return Blacklist.getBlacklistParameters();
  }

  @Override
  StackGresPostgresConfig buildResource(String namespace) {

    StackGresPostgresConfigSpec spec = new StackGresPostgresConfigSpec();
    spec.setPgVersion(defaultPostgresVersion);
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
