/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;

@ApplicationScoped
public class DefaultClusterRestoreFactory
    extends AbstractCustomResourceFactory<StackGresClusterRestore> {

  private static final String DEFAULT_RESTORE_VALUES_FILE = "/restore-default-values.properties";

  @Inject
  public DefaultClusterRestoreFactory(StackGresPropertyContext<OperatorProperty> context) {
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
  StackGresClusterRestore buildResource(String namespace) {
    return buildFromDefaults(StackGresClusterRestore.class);
  }

}
