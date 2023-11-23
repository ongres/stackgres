/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultShardedClusterRestoreFactory
    extends AbstractCustomResourceFactory<StackGresShardedClusterRestore> {

  private static final String DEFAULT_RESTORE_VALUES_FILE = "/restore-default-values.properties";

  @Inject
  public DefaultShardedClusterRestoreFactory(StackGresPropertyContext<OperatorProperty> context) {
    super(context);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return StackGresUtil.loadProperties(DEFAULT_RESTORE_VALUES_FILE);
  }

  @Override
  StackGresShardedClusterRestore buildResource(String namespace) {
    return buildFromDefaults(StackGresShardedClusterRestore.class);
  }

}
