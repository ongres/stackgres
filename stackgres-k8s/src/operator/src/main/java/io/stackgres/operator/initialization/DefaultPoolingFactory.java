/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;

@ApplicationScoped
public class DefaultPoolingFactory
    extends AbstractCustomResourceFactory<StackGresPoolingConfig> {

  @Inject
  public DefaultPoolingFactory(StackGresPropertyContext<OperatorProperty> context) {
    super(context);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  StackGresPoolingConfig buildResource(String namespace) {
    StackGresPoolingConfig config = new StackGresPoolingConfig();
    config.getMetadata().setName(generateDefaultName());
    config.getMetadata().setNamespace(namespace);

    final var spec = new StackGresPoolingConfigSpec();
    final var pgBouncer = new StackGresPoolingConfigPgBouncer();
    final var pgbouncerIni = new StackGresPoolingConfigPgBouncerPgbouncerIni();
    pgbouncerIni.setParameters(Map.of());
    pgBouncer.setPgbouncerIni(pgbouncerIni);
    spec.setPgBouncer(pgBouncer);
    config.setSpec(spec);
    final var status = new StackGresPoolingConfigStatus();
    final var pgBouncerStatus = new StackGresPoolingConfigPgBouncerStatus();
    pgBouncerStatus.setDefaultParameters(getDefaultValues());
    status.setPgBouncer(pgBouncerStatus);
    config.setStatus(status);

    return config;
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return PgBouncerDefaultValues.getProperties();
  }

}
