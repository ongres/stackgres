/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigDefinition;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigSpec;

@ApplicationScoped
public class RestoreConfigFactory extends
    AbstractCustomResourceFactory<StackgresRestoreConfig> {

  public static final String NAME = "defaultrestoreconfig";
  public static final String RESTORE_CONFIG_DEFAULT_VALUES = "restore-default-values.properties";

  @Override
  String getDefaultPropertiesFile() {
    return RESTORE_CONFIG_DEFAULT_VALUES;
  }

  @Override
  StackgresRestoreConfig buildResource(String namespace) {
    StackgresRestoreConfig config = new StackgresRestoreConfig();

    config.setApiVersion(StackgresRestoreConfigDefinition.APIVERSION);
    config.setKind(StackgresRestoreConfigDefinition.KIND);
    config.getMetadata().setNamespace(namespace);
    config.getMetadata().setName(NAME);

    StackgresRestoreConfigSpec spec = buildSpec(StackgresRestoreConfigSpec.class);
    config.setSpec(spec);
    return config;

  }
}
