/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.ClusterControllerProperty.CLUSTER_NAME;
import static io.stackgres.common.ClusterControllerProperty.CLUSTER_NAMESPACE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.ClusterPathV2;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterControllerReconciliationCycle
    extends
    ReconciliationCycle<StackGresClusterContext, StackGresCluster, ClusterResourceHandlerSelector> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterControllerReconciliationCycle.class);

  private final StackGresContext context;
  private final ClusterControllerPropertyContext propertyContext;
  private final EventController eventController;
  private final LabelFactoryForCluster labelFactory;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final Metrics metrics;
  private final ObjectMapper objectMapper;
  private long reconciliationStart;

  @Dependent
  public static class Parameters {
    @Inject
    StackGresContext context;
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
    LabelFactoryForCluster labelFactory;
    @Inject
    CustomResourceFinder<StackGresCluster> clusterFinder;
    @Inject
    Metrics metrics;
    @Inject
    ObjectMapper objectMapper;
  }

  /**
   * Create a {@code ClusterReconciliationCycle} instance.
   */
  @Inject
  public ClusterControllerReconciliationCycle(Parameters parameters) {
    super("Cluster", parameters.client,
        parameters.reconciliator,
        parameters.handlerSelector);
    this.context = parameters.context;
    this.propertyContext = parameters.propertyContext;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.clusterFinder = parameters.clusterFinder;
    this.metrics = parameters.metrics;
    this.objectMapper = parameters.objectMapper;
  }

  public ClusterControllerReconciliationCycle() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.context = null;
    this.propertyContext = null;
    this.eventController = null;
    this.labelFactory = null;
    this.clusterFinder = null;
    this.metrics = null;
    this.objectMapper = null;
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
  protected void onPreReconciliation(StackGresClusterContext context) {
    reconciliationStart = System.currentTimeMillis();
  }

  @Override
  protected void onPostReconciliation(StackGresClusterContext context) {
    metrics.setReconciliationLastDuration(
        StackGresCluster.class,
        System.currentTimeMillis() - reconciliationStart);
    metrics.incrementReconciliationTotalPerformed(StackGresCluster.class);
  }

  @Override
  protected void onError(Exception ex) {
    metrics.incrementReconciliationTotalErrors(StackGresCluster.class);
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
  protected List<HasMetadata> getRequiredResources(
      StackGresClusterContext context) {
    return ResourceGenerator.<StackGresClusterContext>with(context)
        .of(HasMetadata.class)
        .stream()
        .toList();
  }

  @Override
  public StackGresClusterContext getContextWithExistingResourcesOnly(
      StackGresClusterContext context,
      List<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
    return ImmutableStackGresClusterContext.copyOf(context)
        .withExistingResources(existingResourcesOnly);
  }

  @Override
  protected StackGresClusterContext getContextWithExistingAndRequiredResources(
      StackGresClusterContext context,
      List<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      List<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    return ImmutableStackGresClusterContext.copyOf(context)
        .withRequiredResources(requiredResources)
        .withExistingResources(existingResources);
  }

  @Override
  public List<StackGresCluster> getExistingContextResources() {
    return List.of(getExistingCustomResource(
        LOGGER,
        clusterFinder,
        objectMapper,
        propertyContext.getString(CLUSTER_NAMESPACE),
        propertyContext.getString(CLUSTER_NAME)));
  }

  @Override
  public StackGresCluster getExistingContextResource(StackGresCluster source) {
    final String namespace = source.getMetadata().getNamespace();
    final String name = source.getMetadata().getName();
    return getExistingCustomResource(
        LOGGER,
        clusterFinder,
        objectMapper,
        namespace,
        name);
  }

  @Override
  protected StackGresClusterContext getContextFromResource(
      StackGresCluster cluster) {
    return ImmutableStackGresClusterContext.builder()
        .context(context)
        .cluster(cluster)
        .extensions(Optional.ofNullable(cluster.getStatus())
            .map(StackGresClusterStatus::getExtensions)
            .orElse(List.of()))
        .labels(labelFactory.genericLabels(cluster))
        .build();
  }

  static StackGresCluster getExistingCustomResource(
      final Logger logger,
      final CustomResourceFinder<StackGresCluster> clusterFinder,
      final ObjectMapper objectMapper,
      final String namespace,
      final String name) {
    final Optional<Path> latestCustomResourcePath = findLatestCustomResourcePath(namespace, name);
    try {
      return clusterFinder.findByNameAndNamespace(name, namespace)
          .orElseThrow(() -> new IllegalArgumentException(StackGresCluster.KIND
              + " " + name + "." + namespace + " not found"));
    } catch (Exception ex) {
      if (latestCustomResourcePath.isPresent()) {
        try {
          return objectMapper.readValue(
              latestCustomResourcePath.get().toFile(), StackGresCluster.class);
        } catch (Exception jex) {
          ex.addSuppressed(jex);
        }
      }
      if (ex instanceof RuntimeException rex) {
        throw rex;
      }
      throw new RuntimeException(ex);
    }
  }

  public static boolean existsContextResource() {
    return findLatestCustomResourcePath(
        ClusterControllerProperty.CLUSTER_NAMESPACE.getString(),
        ClusterControllerProperty.CLUSTER_NAME.getString())
        .isPresent();
  }

  static void writeCustomResource(
      final Logger logger,
      final ObjectMapper objectMapper,
      final StackGresCluster cluster) {
    final Path latestCustomResourcePath = getLatestCustomResourcePath(cluster);
    try {
      objectMapper.writeValue(latestCustomResourcePath.toFile(), cluster);
    } catch (Exception jex) {
      logger.warn("Error while trying to store latest value of SGCluster to " + latestCustomResourcePath, jex);
    }
  }

  /**
   * Without the SGCluster the registry flag (that selects the base path) is unknown, so the
   * latest custom resource is looked for under both base paths: only the one of the image in
   * use is a mounted volume.
   */
  private static Optional<Path> findLatestCustomResourcePath(
      final String namespace, final String name) {
    return Stream.of(ClusterPathV2.PG_BASE_PATH.path(), ClusterPathV1.PG_BASE_PATH.path())
        .map(basePath -> getLatestCustomResourcePath(basePath, namespace, name))
        .filter(Files::exists)
        .findFirst();
  }

  private static Path getLatestCustomResourcePath(final StackGresCluster cluster) {
    return getLatestCustomResourcePath(
        StackGresUtil.isRegistryEnabled(cluster)
            ? ClusterPathV2.PG_BASE_PATH.path()
            : ClusterPathV1.PG_BASE_PATH.path(),
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName());
  }

  private static Path getLatestCustomResourcePath(
      final String basePath, final String namespace, final String name) {
    return Paths.get(basePath, ".latest." + namespace + "." + name + ".sgcluster.json");
  }

}
