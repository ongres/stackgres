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
import io.stackgres.operator.backup.Backup;
import io.stackgres.operator.common.StackGresClusterConfigTransformer;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.patroni.PatroniConfigEndpoints;
import io.stackgres.operator.patroni.PatroniConfigMap;
import io.stackgres.operator.patroni.PatroniRole;
import io.stackgres.operator.patroni.PatroniSecret;
import io.stackgres.operator.patroni.PatroniServices;
import io.stackgres.operator.patroni.StatefulsetResourceBuilder;

@ApplicationScoped
public class Cluster implements StackGresClusterConfigTransformer<StackGresClusterContext> {

  private final ObjectMapper objectMapper;

  private final StatefulsetResourceBuilder clusterResourcesBuilder;

  private final PatroniConfigMap patroniConfigMap;

  private final PatroniSecret patroniSecret;

  @Inject
  public Cluster(ObjectMapper objectMapper,
                 StatefulsetResourceBuilder clusterResourcesBuilder,
                 PatroniConfigMap patroniConfigMap,
                 PatroniSecret patroniSecret) {
    this.objectMapper = objectMapper;
    this.clusterResourcesBuilder = clusterResourcesBuilder;
    this.patroniConfigMap = patroniConfigMap;
    this.patroniSecret = patroniSecret;
  }

  @Override
  public ImmutableList<HasMetadata> getResources(
      ResourceGeneratorContext<StackGresClusterContext> context) {
    return ImmutableList.<HasMetadata>builder()
        .add(PatroniRole.createServiceAccount(context.getContext()))
        .add(PatroniRole.createRole(context.getContext()))
        .add(PatroniRole.createRoleBinding(context.getContext()))
        .addAll(patroniSecret.create(context.getContext()))
        .addAll(PatroniServices.createServices(context.getContext()))
        .add(PatroniConfigEndpoints.create(context.getContext(), objectMapper))
        .add(patroniConfigMap.create(context.getContext(), objectMapper))
        .addAll(BackupCronJob.create(context))
        .addAll(clusterResourcesBuilder.create(context))
        .addAll(Backup.create(context))
        .build();
  }

}
