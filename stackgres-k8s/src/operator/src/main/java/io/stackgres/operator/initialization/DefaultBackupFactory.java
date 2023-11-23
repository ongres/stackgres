/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultBackupFactory extends AbstractCustomResourceFactory<StackGresBackupConfig> {

  public static final String BACKUP_DEFAULT_VALUES = "/backup-default-values.properties";
  public static final String NAME = "defaultbackupconfig";

  @Inject
  public DefaultBackupFactory(StackGresPropertyContext<OperatorProperty> context) {
    super(context);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  StackGresBackupConfig buildResource(String namespace) {
    StackGresBackupConfig config = new StackGresBackupConfig();

    config.getMetadata().setNamespace(namespace);
    config.getMetadata().setName(generateDefaultName());

    StackGresBackupConfigSpec spec = buildFromDefaults(StackGresBackupConfigSpec.class);
    config.setSpec(spec);
    return config;
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return StackGresUtil.loadProperties(BACKUP_DEFAULT_VALUES);
  }

}
