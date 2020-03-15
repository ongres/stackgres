/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;

@ApplicationScoped
public class DefaultBackupFactory extends AbstractCustomResourceFactory<StackGresBackupConfig> {

  public static final String BACKUP_DEFAULT_VALUES = "backup-default-values.properties";
  public static final String NAME = "defaultbackupconfig";

  @Override
  StackGresBackupConfig buildResource(String namespace) {
    StackGresBackupConfig config = new StackGresBackupConfig();

    config.setApiVersion(StackGresBackupConfigDefinition.APIVERSION);
    config.setKind(StackGresBackupConfigDefinition.KIND);
    config.getMetadata().setNamespace(namespace);
    config.getMetadata().setName(NAME);

    StackGresBackupConfigSpec spec = buildSpec(StackGresBackupConfigSpec.class);
    config.setSpec(spec);
    return config;
  }

  @Override
  String getDefaultPropertiesFile() {
    return BACKUP_DEFAULT_VALUES;
  }

}
