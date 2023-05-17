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
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import org.jetbrains.annotations.NotNull;

@Dependent
public class DefaultPostgresFactory extends AbstractCustomResourceFactory<StackGresPostgresConfig>
    implements PostgresConfigurationFactory {

  private @NotNull String postgresVersion;

  @Inject
  public DefaultPostgresFactory(StackGresPropertyContext<OperatorProperty> context) {
    super(context);
    this.postgresVersion = StackGresComponent.POSTGRESQL.getLatest().getMajorVersion(
        StackGresComponent.LATEST);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  Properties getDefaultPropertiesFile() {
    return PostgresDefaultValues.getProperties(postgresVersion);
  }

  @Override
  StackGresPostgresConfig buildResource(String namespace) {
    Map<String, String> defaultValues = getDefaultValues();
    Set<String> blockedValues = PostgresBlocklist.getBlocklistParameters();
    return new StackGresPostgresConfigBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(namespace)
            .withName(generateDefaultName())
            .build())
        .withNewSpec()
        .withPostgresVersion(postgresVersion)
        .withPostgresqlConf(defaultValues.entrySet()
            .stream()
            .filter(e -> !blockedValues.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .endSpec()
        .withNewStatus()
        .withDefaultParameters(defaultValues)
        .endStatus()
        .build();
  }

  @Override
  public String generateDefaultName() {
    return getDefaultPrefix()
        + System.currentTimeMillis();
  }

  @Override
  public String getDefaultPrefix() {
    return "postgres-"
        + postgresVersion
        + "-"
        + DEFAULT_RESOURCE_NAME_PREFIX;
  }

  @Override
  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(@NotNull String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }
}
