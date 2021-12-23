/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.postgres.PostgresBootstrapReconciliator;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.distributedlogs.common.ClusterBootstrapEventReason;
import io.stackgres.distributedlogs.configuration.DistributedLogsControllerPropertyContext;

@ApplicationScoped
public class DistributedLogsControllerPostgresBootstrapReconciliator
    extends PostgresBootstrapReconciliator {

  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject DistributedLogsControllerPropertyContext propertyContext;
  }

  @Inject
  public DistributedLogsControllerPostgresBootstrapReconciliator(Parameters parameters) {
    super(parameters.endpointsFinder, parameters.propertyContext
        .getString(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME));
    this.eventController = parameters.eventController;
  }

  public static DistributedLogsControllerPostgresBootstrapReconciliator create(
      Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsControllerPostgresBootstrapReconciliator(parameters.findAny().get());
  }

  public DistributedLogsControllerPostgresBootstrapReconciliator() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.eventController = null;
  }

  @Override
  protected void onClusterBootstrapped(KubernetesClient client) {
    eventController.sendEvent(ClusterBootstrapEventReason.CLUSTER_BOOTSTRAP_COMPLETED,
        "Cluster bootstrap completed", client);
  }

}
