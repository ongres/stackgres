/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigSpec;

@ApplicationScoped
public class DefaultPgBouncerFactory
    extends AbstractCustomResourceFactory<StackGresPgbouncerConfig> {

  public static final String NAME = "defaultpgbouncer";
  public static final String PGBOUNCER_DEFAULT_VALUES = "pgbouncer-default-values.properties";

  @Override
  StackGresPgbouncerConfig buildResource(String namespace) {

    StackGresPgbouncerConfig config = new StackGresPgbouncerConfig();
    config.setApiVersion(StackGresPgbouncerConfigDefinition.APIVERSION);
    config.setKind(StackGresPgbouncerConfigDefinition.KIND);
    config.getMetadata().setName(NAME);
    config.getMetadata().setNamespace(namespace);

    StackGresPgbouncerConfigSpec spec = new StackGresPgbouncerConfigSpec();
    spec.setPgbouncerConf(getDefaultValues());
    config.setSpec(spec);

    return config;
  }

  @Override
  String getDefaultPropertiesFile() {
    return PGBOUNCER_DEFAULT_VALUES;
  }
}
