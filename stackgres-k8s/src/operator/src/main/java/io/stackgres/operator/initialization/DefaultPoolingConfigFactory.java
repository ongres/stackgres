/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPoolingConfigFactory
    extends DefaultCustomResourceFactory<StackGresPoolingConfig, HasMetadata> {

  @Override
  protected String getDefaultPropertyResourceName(HasMetadata source) {
    return "default";
  }

  @Override
  protected Properties loadDefaultProperties(String defaultPropertyResourceName) {
    return PgBouncerDefaultValues.getProperties();
  }

  @Override
  public StackGresPoolingConfig buildResource(HasMetadata resource) {
    Map<String, String> defaultValues = getDefaultValues(resource);
    Set<String> blockedValues = PgBouncerBlocklist.getBlocklistParameters();
    return new StackGresPoolingConfigBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(resource.getMetadata().getNamespace())
            .withName(getDefaultResourceName(resource))
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

}
