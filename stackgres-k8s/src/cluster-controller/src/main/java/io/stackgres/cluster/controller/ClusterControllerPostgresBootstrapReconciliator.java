/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterBootstrapEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.postgres.PostgresBootstrapReconciliator;
import io.stackgres.common.resource.ResourceFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterControllerPostgresBootstrapReconciliator
    extends PostgresBootstrapReconciliator {

  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public ClusterControllerPostgresBootstrapReconciliator(Parameters parameters) {
    super(parameters.endpointsFinder, parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME));
    this.eventController = parameters.eventController;
  }

  public static ClusterControllerPostgresBootstrapReconciliator create(
      Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new ClusterControllerPostgresBootstrapReconciliator(parameters.findAny().get());
  }

  @Override
  protected void onClusterBootstrapped(KubernetesClient client) {
    eventController.sendEvent(ClusterBootstrapEventReason.CLUSTER_BOOTSTRAP_COMPLETED,
        "Cluster bootstrap completed", client);
  }

}
