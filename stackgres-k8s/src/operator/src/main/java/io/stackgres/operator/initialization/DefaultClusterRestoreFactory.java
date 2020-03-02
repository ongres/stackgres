/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;

@ApplicationScoped
public class DefaultClusterRestoreFactory
    extends AbstractCustomResourceFactory<StackGresClusterRestore> {

  private static final String DEFAULT_RESTORE_VALUES_FILE = "restore-default-values.properties";

  @Override
  String getDefaultPropertiesFile() {
    return DEFAULT_RESTORE_VALUES_FILE;
  }

  @Override
  StackGresClusterRestore buildResource(String namespace) {
    return buildSpec(StackGresClusterRestore.class);
  }
}
