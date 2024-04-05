/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterBootstrapEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.postgres.PostgresBootstrapReconciliator;
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
    @Inject PatroniCtl patroniCtl;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public ClusterControllerPostgresBootstrapReconciliator(Parameters parameters) {
    super(parameters.patroniCtl, parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME));
    this.eventController = parameters.eventController;
  }

  @Override
  protected void onClusterBootstrapped(KubernetesClient client) {
    eventController.sendEvent(ClusterBootstrapEventReason.CLUSTER_BOOTSTRAP_COMPLETED,
        "Cluster bootstrap completed", client);
  }

}
