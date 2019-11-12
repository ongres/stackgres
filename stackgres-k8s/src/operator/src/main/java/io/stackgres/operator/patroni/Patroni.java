/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.common.StackGresClusterConfigTransformer;

@ApplicationScoped
public class Patroni implements StackGresClusterConfigTransformer {

  private final ObjectMapper objectMapper;

  @Inject
  public Patroni(ObjectMapperProvider objectMapperProvider) {
    this.objectMapper = objectMapperProvider.objectMapper();
  }

  @Override
  public ImmutableList<HasMetadata> getResources(StackGresClusterConfig config) {
    return ImmutableList.<HasMetadata>builder()
        .add(PatroniRole.createServiceAccount(config.getCluster()))
        .add(PatroniRole.createRole(config.getCluster()))
        .add(PatroniRole.createRoleBinding(config.getCluster()))
        .add(PatroniSecret.create(config.getCluster()))
        .addAll(PatroniServices.createServices(config.getCluster()))
        .add(PatroniConfigEndpoints.create(config, objectMapper))
        .add(PatroniConfigMap.create(config, objectMapper))
        .addAll(StackGresStatefulSet.create(config))
        .build();
  }

}
