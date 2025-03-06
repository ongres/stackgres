/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultClusterRestoreFactory
    extends DefaultLoaderFactory<StackGresClusterRestore> {

  private static final String DEFAULT_RESTORE_VALUES_FILE = "/restore-default-values.properties";

  @Override
  Properties getDefaultPropertiesFile() {
    return StackGresUtil.loadProperties(DEFAULT_RESTORE_VALUES_FILE);
  }

  @Override
  public StackGresClusterRestore buildResource() {
    return buildFromDefaults(StackGresClusterRestore.class);
  }

}
