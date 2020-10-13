/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.ObjectMapperProvider;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.ClusterResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.ResourceGeneratorReconciliator;

@ApplicationScoped
public class ClusterReconciliator
    extends ResourceGeneratorReconciliator<StackGresClusterContext, StackGresCluster,
      ClusterResourceHandlerSelector> {

  private final ClusterStatusManager statusManager;
  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject ClusterResourceHandlerSelector handlerSelector;
    @Inject ObjectMapperProvider objectMapperProvider;
    @Inject ClusterStatusManager statusManager;
    @Inject EventController eventController;
  }

  @Inject
  public ClusterReconciliator(Parameters parameters) {
    super("Cluster", StackGresClusterContext::getCluster,
        parameters.handlerSelector, parameters.objectMapperProvider.objectMapper());
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
  }

  public ClusterReconciliator() {
    super(null, c -> null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.statusManager = null;
    this.eventController = null;
  }

  public static ClusterReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new ClusterReconciliator(parameters.findAny().get());
  }

  @Override
  protected void onConfigCreated(KubernetesClient client, StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
        + cluster.getMetadata().getName() + " created", cluster, client);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), context, client);
  }

  @Override
  protected void onConfigUpdated(KubernetesClient client, StackGresClusterContext context) {
    StackGresCluster cluster = context.getCluster();
    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
        + cluster.getMetadata().getName() + " updated", cluster, client);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), context, client);
  }

  @Override
  protected void onPostConfigReconcilied(KubernetesClient client, StackGresClusterContext context) {
    statusManager.updatePendingRestart(context);
  }

}
