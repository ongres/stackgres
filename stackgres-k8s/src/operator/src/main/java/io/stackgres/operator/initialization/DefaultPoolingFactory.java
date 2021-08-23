/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;

@ApplicationScoped
public class DefaultPoolingFactory
    extends AbstractCustomResourceFactory<StackGresPoolingConfig> {

  public static final String PGBOUNCER_DEFAULT_VALUES = "pgbouncer-default-values.properties";

  @Override
  StackGresPoolingConfig buildResource(String namespace) {

    StackGresPoolingConfig config = new StackGresPoolingConfig();
    config.getMetadata().setName(generateDefaultName());
    config.getMetadata().setNamespace(namespace);

    StackGresPoolingConfigSpec spec = new StackGresPoolingConfigSpec();
    final StackGresPoolingConfigPgBouncer pgBouncer = new StackGresPoolingConfigPgBouncer();
    final StackGresPoolingConfigPgBouncerPgbouncerIni pgbouncerIni
        = new StackGresPoolingConfigPgBouncerPgbouncerIni();
    pgbouncerIni.setParameters(getDefaultValues());
    pgBouncer.setPgbouncerIni(pgbouncerIni);
    spec.setPgBouncer(pgBouncer);
    config.setSpec(spec);

    return config;
  }

  @Override
  String getDefaultPropertiesFile() {
    return PGBOUNCER_DEFAULT_VALUES;
  }

}
