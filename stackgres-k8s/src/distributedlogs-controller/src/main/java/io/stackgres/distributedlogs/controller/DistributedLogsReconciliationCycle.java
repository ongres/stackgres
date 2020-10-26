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

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.distributedlogs.common.DistributedLogsEventReason;
import io.stackgres.distributedlogs.common.DistributedLogsProperty;
import io.stackgres.distributedlogs.common.ImmutableStackGresDistributedLogsContext;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.distributedlogs.configuration.DistributedLogsPropertyContext;
import io.stackgres.distributedlogs.resource.DistributedLogsResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsReconciliationCycle
    extends ReconciliationCycle<StackGresDistributedLogsContext,
      StackGresDistributedLogs, DistributedLogsResourceHandlerSelector> {

  private final DistributedLogsPropertyContext propertyContext;
  private final EventController eventController;
  private final LabelFactory<StackGresDistributedLogs> labelFactory;
  private final CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;

  @Dependent
  public static class Parameters {
    @Inject KubernetesClientFactory clientFactory;
    @Inject DistributedLogsReconciliator reconciliator;
    @Inject DistributedLogsResourceHandlerSelector handlerSelector;
    @Inject DistributedLogsPropertyContext propertyContext;
    @Inject EventController eventController;
    @Inject LabelFactory<StackGresDistributedLogs> labelFactory;
    @Inject CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;
  }

  /**
   * Create a {@code DistributeLogsReconciliationCycle} instance.
   */
  @Inject
  public DistributedLogsReconciliationCycle(Parameters parameters) {
    super("DistributeLogs", parameters.clientFactory::create,
        parameters.reconciliator,
        StackGresDistributedLogsContext::getDistributedLogs,
        parameters.handlerSelector);
    this.propertyContext = parameters.propertyContext;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.distributedLogsFinder = parameters.distributedLogsFinder;
  }

  public DistributedLogsReconciliationCycle() {
    super(null, null, null, c -> null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.propertyContext = null;
    this.eventController = null;
    this.labelFactory = null;
    this.distributedLogsFinder = null;
  }

  public static DistributedLogsReconciliationCycle create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsReconciliationCycle(parameters.findAny().get());
  }

  @Override
  protected void onError(Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres DistributeLogs reconciliation cycle failed",
        new String[] {
        }).getMessage();
    logger.error(message, ex);
    try (KubernetesClient client = clientSupplier.get()) {
      eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), client);
    }
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
    try (KubernetesClient client = clientSupplier.get()) {
      eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), configResource, client);
    }
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
  protected ImmutableList<StackGresDistributedLogsContext> getExistingContexts() {
    return distributedLogsFinder.findByNameAndNamespace(
        propertyContext.getString(DistributedLogsProperty.DISTRIBUTEDLOGS_NAME),
        propertyContext.getString(DistributedLogsProperty.DISTRIBUTEDLOGS_NAMESPACE))
        .stream()
        .map(distributedLogs -> getDistributedLogsContext(distributedLogs))
        .collect(ImmutableList.toImmutableList());
  }

  private StackGresDistributedLogsContext getDistributedLogsContext(
      StackGresDistributedLogs distributedLogs) {
    final StackGresCluster cluster = getStackGresCLusterForDistributedLogs(distributedLogs);
    return ImmutableStackGresDistributedLogsContext.builder()
        .distributedLogs(distributedLogs)
        .cluster(cluster)
        .labels(labelFactory.clusterLabels(cluster))
        .build();
  }

  private StackGresCluster getStackGresCLusterForDistributedLogs(
      StackGresDistributedLogs distributedLogs) {
    final StackGresCluster distributedLogsCluster = new StackGresCluster();
    distributedLogsCluster.getMetadata().setNamespace(
        distributedLogs.getMetadata().getNamespace());
    distributedLogsCluster.getMetadata().setName(
        distributedLogs.getMetadata().getName());
    distributedLogsCluster.getMetadata().setUid(
        distributedLogs.getMetadata().getUid());
    return distributedLogsCluster;
  }

}
