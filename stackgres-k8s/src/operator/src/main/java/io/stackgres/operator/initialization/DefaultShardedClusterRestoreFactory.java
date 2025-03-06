/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultShardedClusterRestoreFactory
    extends DefaultLoaderFactory<StackGresShardedClusterRestore> {

  private static final String DEFAULT_RESTORE_VALUES_FILE = "/restore-default-values.properties";

  @Override
  Properties getDefaultPropertiesFile() {
    return StackGresUtil.loadProperties(DEFAULT_RESTORE_VALUES_FILE);
  }

  @Override
  public StackGresShardedClusterRestore buildResource() {
    return buildFromDefaults(StackGresShardedClusterRestore.class);
  }

}
