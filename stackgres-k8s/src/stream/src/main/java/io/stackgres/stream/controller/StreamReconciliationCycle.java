/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.controller;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StreamEventReason;
import io.stackgres.common.labels.LabelFactory;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.stream.app.StreamProperty;
import io.stackgres.stream.common.ImmutableStackGresStreamContext;
import io.stackgres.stream.common.StackGresStreamContext;
import io.stackgres.stream.configuration.StreamPropertyContext;
import io.stackgres.stream.resource.StreamResourceHandlerSelector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class StreamReconciliationCycle
    extends
    ReconciliationCycle<StackGresStreamContext, StackGresStream, StreamResourceHandlerSelector> {

  private final StreamPropertyContext propertyContext;
  private final EventController eventController;
  private final LabelFactory<StackGresStream> labelFactory;
  private final CustomResourceFinder<StackGresStream> streamFinder;

  @Dependent
  public static class Parameters {
    @Inject
    KubernetesClient client;
    @Inject
    StreamReconciliator reconciliator;
    @Inject
    StreamResourceHandlerSelector handlerSelector;
    @Inject
    StreamPropertyContext propertyContext;
    @Inject
    EventController eventController;
    @Inject
    LabelFactory<StackGresStream> labelFactory;
    @Inject
    CustomResourceFinder<StackGresStream> streamFinder;
  }

  /**
   * Create a {@code ClusterReconciliationCycle} instance.
   */
  @Inject
  public StreamReconciliationCycle(Parameters parameters) {
    super("Stream", parameters.client,
        parameters.reconciliator,
        parameters.handlerSelector);
    this.propertyContext = parameters.propertyContext;
    this.eventController = parameters.eventController;
    this.labelFactory = parameters.labelFactory;
    this.streamFinder = parameters.streamFinder;
  }

  public StreamReconciliationCycle() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.propertyContext = null;
    this.eventController = null;
    this.labelFactory = null;
    this.streamFinder = null;
  }

  public static StreamReconciliationCycle create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new StreamReconciliationCycle(parameters.findAny().get());
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
    eventController.sendEvent(StreamEventReason.STREAM_ERROR,
        message + ": " + ex.getMessage(), client);
  }

  @Override
  protected void onConfigError(StackGresStreamContext context,
      HasMetadata configResource, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres Cluster {}.{} reconciliation failed",
        new String[] {
            configResource.getMetadata().getNamespace(),
            configResource.getMetadata().getName(),
        }).getMessage();
    logger.error(message, ex);
    eventController.sendEvent(StreamEventReason.STREAM_ERROR,
        message + ": " + ex.getMessage(), configResource, client);
  }

  @Override
  protected List<HasMetadata> getRequiredResources(
      StackGresStreamContext context) {
    return ResourceGenerator.<StackGresStreamContext>with(context)
        .of(HasMetadata.class)
        .stream()
        .toList();
  }

  @Override
  protected StackGresStreamContext getContextWithExistingResourcesOnly(
      StackGresStreamContext context,
      List<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
    return ImmutableStackGresStreamContext.copyOf(context)
        .withExistingResources(existingResourcesOnly);
  }

  @Override
  protected StackGresStreamContext getContextWithExistingAndRequiredResources(
      StackGresStreamContext context,
      List<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      List<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    return ImmutableStackGresStreamContext.copyOf(context)
        .withRequiredResources(requiredResources)
        .withExistingResources(existingResources);
  }

  @Override
  public List<StackGresStream> getExistingContextResources() {
    return streamFinder.findByNameAndNamespace(
        propertyContext.getString(StreamProperty.STREAM_NAME),
        propertyContext.getString(StreamProperty.STREAM_NAMESPACE))
        .stream()
        .toList();
  }

  @Override
  public StackGresStream getExistingContextResource(StackGresStream source) {
    final String namespace = source.getMetadata().getNamespace();
    final String name = source.getMetadata().getName();
    return streamFinder.findByNameAndNamespace(
        name,
        namespace)
        .orElseThrow(() -> new IllegalArgumentException(StackGresStream.KIND
            + " " + name + "." + namespace + " not found"));
  }

  @Override
  protected StackGresStreamContext getContextFromResource(
      StackGresStream stream) {
    return ImmutableStackGresStreamContext.builder()
        .stream(stream)
        .labels(labelFactory.genericLabels(stream))
        .build();
  }

}
