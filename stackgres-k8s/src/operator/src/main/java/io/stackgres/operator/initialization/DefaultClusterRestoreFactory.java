/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;

@ApplicationScoped
public class DefaultClusterRestoreFactory
    extends AbstractCustomResourceFactory<StackGresClusterRestore> {

  private static final String DEFAULT_RESTORE_VALUES_FILE = "/restore-default-values.properties";

  @Override
  Properties getDefaultPropertiesFile() {
    return StackGresUtil.loadProperties(DEFAULT_RESTORE_VALUES_FILE);
  }

  @Override
  StackGresClusterRestore buildResource(String namespace) {
    return buildFromDefaults(StackGresClusterRestore.class);
  }

}
