/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresClusterConfigTransformer;
import io.stackgres.operator.app.ObjectMapperProvider;

@ApplicationScoped
public class Patroni implements StackGresClusterConfigTransformer {

  @Inject
  ObjectMapperProvider objectMapperProvider;

  @Override
  public List<HasMetadata> getResources(StackGresClusterConfig config) {
    return ImmutableList.<HasMetadata>builder()
        .add(PatroniRole.createServiceAccount(config.getCluster()))
        .add(PatroniRole.createRole(config.getCluster()))
        .add(PatroniRole.createRoleBinding(config.getCluster()))
        .add(PatroniSecret.create(config.getCluster()))
        .addAll(PatroniServices.createServices(config.getCluster()))
        .add(PatroniEndpoints.create(config, objectMapperProvider.objectMapper()))
        .add(PatroniConfigMap.create(config, objectMapperProvider.objectMapper()))
        .addAll(StackGresStatefulSet.create(config))
        .build();
  }

  public boolean isManaged(StackGresClusterConfig config, HasMetadata sgResource) {
    return PatroniSecret.is(config.getCluster(), sgResource)
        || PatroniEndpoints.is(config.getCluster(), sgResource);
  }

}
