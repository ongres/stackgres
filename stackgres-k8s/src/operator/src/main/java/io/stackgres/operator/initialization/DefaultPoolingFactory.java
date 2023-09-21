/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
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
    Map<String, String> defaultValues = getDefaultValues();
    Set<String> blockedValues = PgBouncerBlocklist.getBlocklistParameters();
    return new StackGresPoolingConfigBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(namespace)
            .withName(generateDefaultName())
            .build())
        .withNewSpec()
        .withNewPgBouncer()
        .withNewPgbouncerIni()
        .withPgbouncer(defaultValues.entrySet()
            .stream()
            .filter(e -> !blockedValues.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .endPgbouncerIni()
        .endPgBouncer()
        .endSpec()
        .withNewStatus()
        .withNewPgBouncer()
        .withDefaultParameters(defaultValues)
        .endPgBouncer()
        .endStatus()
        .build();
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return PgBouncerDefaultValues.getProperties();
  }

}
