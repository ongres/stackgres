/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.StackGresClusterConfigTransformer;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.patroni.PatroniConfigEndpoints;
import io.stackgres.operator.patroni.PatroniConfigMap;
import io.stackgres.operator.patroni.PatroniRole;
import io.stackgres.operator.patroni.PatroniSecret;
import io.stackgres.operator.patroni.PatroniServices;

@ApplicationScoped
public class Cluster implements StackGresClusterConfigTransformer {

  private final ObjectMapper objectMapper;

  @Inject
  public Cluster(ObjectMapperProvider objectMapperProvider) {
    this.objectMapper = objectMapperProvider.objectMapper();
  }

  @Override
  public ImmutableList<HasMetadata> getResources(ResourceGeneratorContext context) {
    return ImmutableList.<HasMetadata>builder()
        .add(PatroniRole.createServiceAccount(context.getClusterConfig().getCluster()))
        .add(PatroniRole.createRole(context.getClusterConfig().getCluster()))
        .add(PatroniRole.createRoleBinding(context.getClusterConfig().getCluster()))
        .add(PatroniSecret.create(context.getClusterConfig().getCluster()))
        .addAll(PatroniServices.createServices(context.getClusterConfig().getCluster()))
        .add(PatroniConfigEndpoints.create(context.getClusterConfig(), objectMapper))
        .add(PatroniConfigMap.create(context.getClusterConfig(), objectMapper))
        .addAll(StackGresStatefulSet.create(context))
        .build();
  }

}
