/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

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
import io.stackgres.common.CdiUtil;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.distributedlogs.common.DistributedLogsControllerEventReason;
import io.stackgres.distributedlogs.common.ImmutableStackGresDistributedLogsContext;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.distributedlogs.configuration.DistributedLogsControllerPropertyContext;
import io.stackgres.distributedlogs.resource.DistributedLogsResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsControllerReconciliationCycle
    extends
    ReconciliationCycle<StackGresDistributedLogsContext, StackGresDistributedLogs,
    DistributedLogsResourceHandlerSelector> {

  private final DistributedLogsControllerPropertyContext propertyContext;
  private final EventController eventController;
  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;
  private final CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;

  @Dependent
  public static class Parameters {
    @Inject
    KubernetesClient client;
    @Inject
    DistributedLogsControllerReconciliator reconciliator;
    @Inject
    DistributedLogsResourceHandlerSelector handlerSelector;
    @Inject
    DistributedLogsControllerPropertyContext propertyContext;
    @Inject
    EventController eventController;
    @Inject
    LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;
    @Inject
    CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;
  }

  /**
   * Create a {@code DistributeLogsReconciliationCycle} instance.
   */
  @Inject
  public DistributedLogsControllerReconciliationCycle(Parameters parameters) {
    super("DistributeLogs", parameters.client,
        parameters.reconciliator,
        parameters.handlerSelector);
    this.propertyContext = parameters.propertyContext;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.distributedLogsFinder = parameters.distributedLogsFinder;
  }

  public DistributedLogsControllerReconciliationCycle() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.propertyContext = null;
    this.eventController = null;
    this.labelFactory = null;
    this.distributedLogsFinder = null;
  }

  public static DistributedLogsControllerReconciliationCycle create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsControllerReconciliationCycle(parameters.findAny().get());
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
        "StackGres DistributeLogs reconciliation cycle failed",
        new String[] {
        }).getMessage();
    logger.error(message, ex);
    eventController.sendEvent(
        DistributedLogsControllerEventReason.DISTRIBUTEDLOGS_CONTROLLER_ERROR,
        message + ": " + ex.getMessage(), client);
  }

  @Override
  protected void onConfigError(StackGresDistributedLogsContext context,
      HasMetadata configResource, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres DistributeLogs {}.{} reconciliation failed",
        new String[] {
            configResource.getMetadata().getNamespace(),
            configResource.getMetadata().getName(),
        }).getMessage();
    logger.error(message, ex);
    eventController.sendEvent(
        DistributedLogsControllerEventReason.DISTRIBUTEDLOGS_CONTROLLER_ERROR,
        message + ": " + ex.getMessage(), configResource, client);
  }

  @Override
  protected ImmutableList<HasMetadata> getRequiredResources(
      StackGresDistributedLogsContext context) {
    return ResourceGenerator.<StackGresDistributedLogsContext>with(context)
        .of(HasMetadata.class)
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected StackGresDistributedLogsContext getContextWithExistingResourcesOnly(
      StackGresDistributedLogsContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
    return ImmutableStackGresDistributedLogsContext.copyOf(context)
        .withExistingResources(existingResourcesOnly);
  }

  @Override
  protected StackGresDistributedLogsContext getContextWithExistingAndRequiredResources(
      StackGresDistributedLogsContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    return ImmutableStackGresDistributedLogsContext.copyOf(context)
        .withRequiredResources(requiredResources)
        .withExistingResources(existingResources);
  }

  @Override
  protected ImmutableList<StackGresDistributedLogs> getExistingContextResources() {
    return distributedLogsFinder.findByNameAndNamespace(
        propertyContext.getString(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME),
        propertyContext.getString(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE))
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public StackGresDistributedLogsContext getContextFromResource(
      StackGresDistributedLogs distributedLogs) {
    final StackGresCluster cluster = StackGresDistributedLogsUtil
        .getStackGresClusterForDistributedLogs(distributedLogs);
    return ImmutableStackGresDistributedLogsContext.builder()
        .distributedLogs(distributedLogs)
        .cluster(cluster)
        .extensions(Optional.ofNullable(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions)
            .orElse(List.of()))
        .labels(labelFactory.genericLabels(distributedLogs))
        .build();
  }

}
