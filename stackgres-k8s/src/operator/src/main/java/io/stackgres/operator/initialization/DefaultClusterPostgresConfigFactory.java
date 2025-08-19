/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresDefaultValues;
import jakarta.enterprise.context.Dependent;

@Dependent
public class DefaultClusterPostgresConfigFactory
    extends DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresCluster> {

  @Override
  protected String getDefaultPropertyResourceName(StackGresCluster source) {
    return getPostgresMajorVersion(source);
  }

  @Override
  protected Properties loadDefaultProperties(String defaultPropertyResourceName) {
    return PostgresDefaultValues.getProperties(defaultPropertyResourceName);
  }

  @Override
  public StackGresPostgresConfig buildResource(StackGresCluster resource) {
    if (resource.getStatus() == null
        || resource.getStatus().getPostgresVersion() == null) {
      return new StackGresPostgresConfigBuilder()
          .withMetadata(new ObjectMetaBuilder()
              .withNamespace(resource.getMetadata().getNamespace())
              .withName(getDefaultResourceName(resource))
              .build())
          .withNewSpec()
          .endSpec()
          .build();
    }
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
  public String getDefaultResourceName(StackGresCluster resource) {
    return resource.getMetadata().getName()
        + "-" + getPostgresMajorVersion(resource)
        + DEFAULT_SUFFIX;
  }

  private String getPostgresMajorVersion(StackGresCluster resource) {
    String version = getPostgresFlavorComponent(resource).get(resource)
        .getVersion(resource.getStatus().getPostgresVersion());
    return version.split("\\.")[0];
  }

}
