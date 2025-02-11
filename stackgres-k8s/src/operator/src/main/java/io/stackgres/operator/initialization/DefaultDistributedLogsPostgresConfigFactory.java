/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresDefaultValues;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsCluster;
import jakarta.enterprise.context.Dependent;

@Dependent
public class DefaultDistributedLogsPostgresConfigFactory
    extends DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresDistributedLogs> {

  private LabelFactoryForDistributedLogs labelFactory;

  public DefaultDistributedLogsPostgresConfigFactory(LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected String getDefaultPropertyResourceName(StackGresDistributedLogs source) {
    return getPostgresMajorVersion(source);
  }

  @Override
  protected Properties loadDefaultProperties(String defaultPropertyResourceName) {
    return PostgresDefaultValues.getProperties(defaultPropertyResourceName);
  }

  @Override
  public StackGresPostgresConfig buildResource(StackGresDistributedLogs resource) {
    Map<String, String> defaultValues = getDefaultValues(resource);
    Set<String> blockedValues = PostgresBlocklist.getBlocklistParameters();
    return new StackGresPostgresConfigBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(resource.getMetadata().getNamespace())
            .withName(getDefaultResourceName(resource))
            .build())
        .withNewSpec()
        .withPostgresVersion(getPostgresMajorVersion(resource))
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
  public String getDefaultResourceName(StackGresDistributedLogs resource) {
    return resource.getMetadata().getName()
        + "-" + getPostgresMajorVersion(resource)
        + DEFAULT_SUFFIX;
  }

  private String getPostgresMajorVersion(StackGresDistributedLogs resource) {
    String version = DistributedLogsCluster
        .getCluster(
            labelFactory, resource, Optional.empty())
        .getSpec().getPostgres().getVersion();
    return version.split("\\.")[0];
  }

}
