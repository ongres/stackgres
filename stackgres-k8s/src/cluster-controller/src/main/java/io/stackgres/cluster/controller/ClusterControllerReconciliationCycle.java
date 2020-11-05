/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.List;
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
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterControllerReconciliationCycle
    extends ReconciliationCycle<StackGresClusterContext,
      StackGresCluster, ClusterResourceHandlerSelector> {

  private final ClusterControllerPropertyContext propertyContext;
  private final EventController eventController;
  private final LabelFactory<StackGresCluster> labelFactory;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final ClusterExtensionMetadataManager clusterExtensionMetadataManager;

  @Dependent
  public static class Parameters {
    @Inject KubernetesClientFactory clientFactory;
    @Inject ClusterControllerReconciliator reconciliator;
    @Inject ClusterResourceHandlerSelector handlerSelector;
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject EventController eventController;
    @Inject LabelFactory<StackGresCluster> labelFactory;
    @Inject CustomResourceFinder<StackGresCluster> clusterFinder;
    @Inject ClusterExtensionMetadataManager clusterExtensionMetadataManager;
  }

  /**
   * Create a {@code ClusterReconciliationCycle} instance.
   */
  @Inject
  public ClusterControllerReconciliationCycle(Parameters parameters) {
    super("Cluster", parameters.clientFactory::create,
        parameters.reconciliator,
        StackGresClusterContext::getCluster,
        parameters.handlerSelector);
    this.propertyContext = parameters.propertyContext;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.clusterFinder = parameters.clusterFinder;
    this.clusterExtensionMetadataManager = parameters.clusterExtensionMetadataManager;
  }

  public ClusterControllerReconciliationCycle() {
    super(null, null, null, c -> null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.propertyContext = null;
    this.eventController = null;
    this.labelFactory = null;
    this.clusterFinder = null;
    this.clusterExtensionMetadataManager = null;
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
    try (KubernetesClient client = clientSupplier.get()) {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), client);
    }
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
    try (KubernetesClient client = clientSupplier.get()) {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), configResource, client);
    }
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
  protected ImmutableList<StackGresClusterContext> getExistingContexts() {
    return clusterFinder.findByNameAndNamespace(
        propertyContext.getString(ClusterControllerProperty.CLUSTER_NAME),
        propertyContext.getString(ClusterControllerProperty.CLUSTER_NAMESPACE))
        .stream()
        .map(this::getClusterContext)
        .collect(ImmutableList.toImmutableList());
  }

  private StackGresClusterContext getClusterContext(
      StackGresCluster cluster) {
    Optional<List<StackGresClusterExtension>> extensions =
        Optional.ofNullable(cluster.getSpec().getPostgresExtensions());

    return ImmutableStackGresClusterContext.builder()
        .cluster(cluster)
        .extensions(Seq.seq(extensions.stream())
            .flatMap(List::stream)
            .append(Seq.seq(getDefaultExtensions(cluster))
                .filter(defaultExtension -> extensions.stream()
                    .flatMap(List::stream)
                    .noneMatch(extension -> extension.getName()
                        .equals(defaultExtension.getName())))))
        .labels(labelFactory.clusterLabels(cluster))
        .build();
  }

  private ImmutableList<StackGresClusterExtension> getDefaultExtensions(StackGresCluster cluster) {
    return ImmutableList.of(
        getExtension(cluster, "plpgsql"),
        getExtension(cluster, "pg_stat_statements"),
        getExtension(cluster, "dblink"),
        getExtension(cluster, "plpython3u"));
  }

  private StackGresClusterExtension getExtension(StackGresCluster cluster, String extensionName) {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(extensionName);
    extension.setVersion(Unchecked.supplier(
        () -> clusterExtensionMetadataManager.getExtensionCandidateAnyVersion(
            cluster, extension)).get().getVersion().getVersion());
    return extension;
  }
}
