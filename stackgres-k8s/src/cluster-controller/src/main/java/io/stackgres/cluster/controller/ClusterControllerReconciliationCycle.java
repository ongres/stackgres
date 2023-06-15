/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.ImmutableStackGresClusterContext;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.cluster.resource.ClusterResourceHandlerSelector;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterControllerReconciliationCycle
    extends
    ReconciliationCycle<StackGresClusterContext, StackGresCluster, ClusterResourceHandlerSelector> {

  private final ClusterControllerPropertyContext propertyContext;
  private final EventController eventController;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Dependent
  public static class Parameters {
    @Inject
    KubernetesClient client;
    @Inject
    ClusterControllerReconciliator reconciliator;
    @Inject
    ClusterResourceHandlerSelector handlerSelector;
    @Inject
    ClusterControllerPropertyContext propertyContext;
    @Inject
    EventController eventController;
    @Inject
    LabelFactoryForCluster<StackGresCluster> labelFactory;
    @Inject
    CustomResourceFinder<StackGresCluster> clusterFinder;
  }

  /**
   * Create a {@code ClusterReconciliationCycle} instance.
   */
  @Inject
  public ClusterControllerReconciliationCycle(Parameters parameters) {
    super("Cluster", parameters.client,
        parameters.reconciliator,
        parameters.handlerSelector);
    this.propertyContext = parameters.propertyContext;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.clusterFinder = parameters.clusterFinder;
  }

  public ClusterControllerReconciliationCycle() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.propertyContext = null;
    this.eventController = null;
    this.labelFactory = null;
    this.clusterFinder = null;
  }

  public static ClusterControllerReconciliationCycle create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new ClusterControllerReconciliationCycle(parameters.findAny().get());
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void onError(Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres Cluster reconciliation cycle failed",
        new String[] {
        }).getMessage();
    logger.error(message, ex);
    eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
        message + ": " + ex.getMessage(), client);
  }

  @Override
  protected void onConfigError(StackGresClusterContext context,
      HasMetadata configResource, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres Cluster {}.{} reconciliation failed",
        new String[] {
            configResource.getMetadata().getNamespace(),
            configResource.getMetadata().getName(),
        }).getMessage();
    logger.error(message, ex);
    eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
        message + ": " + ex.getMessage(), configResource, client);
  }

  @Override
  protected ImmutableList<HasMetadata> getRequiredResources(
      StackGresClusterContext context) {
    return ResourceGenerator.<StackGresClusterContext>with(context)
        .of(HasMetadata.class)
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected StackGresClusterContext getContextWithExistingResourcesOnly(
      StackGresClusterContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
    return ImmutableStackGresClusterContext.copyOf(context)
        .withExistingResources(existingResourcesOnly);
  }

  @Override
  protected StackGresClusterContext getContextWithExistingAndRequiredResources(
      StackGresClusterContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    return ImmutableStackGresClusterContext.copyOf(context)
        .withRequiredResources(requiredResources)
        .withExistingResources(existingResources);
  }

  @Override
  public ImmutableList<StackGresCluster> getExistingContextResources() {
    return clusterFinder.findByNameAndNamespace(
        propertyContext.getString(ClusterControllerProperty.CLUSTER_NAME),
        propertyContext.getString(ClusterControllerProperty.CLUSTER_NAMESPACE))
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected StackGresClusterContext getContextFromResource(
      StackGresCluster cluster) {
    return ImmutableStackGresClusterContext.builder()
        .cluster(cluster)
        .extensions(Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getToInstallPostgresExtensions)
            .orElse(ImmutableList.of()))
        .labels(labelFactory.genericLabels(cluster))
        .build();
  }

}
