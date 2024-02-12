/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniEndpointsReconciliator extends SafeReconciliator<ClusterContext, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniEndpointsReconciliator.class);

  private final String podName;
  private final String podIp;
  private final PatroniCtl patroniCtl;
  private final ResourceFinder<Endpoints> endpointsFinder;
  private final ResourceWriter<Endpoints> endpointsWriter;
  private final AtomicReference<Tuple2<EndpointSubset, EndpointSubset>> lastEnpointSubset;

  @Dependent
  public static class Parameters {
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject PatroniCtl patroniCtl;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject ResourceWriter<Endpoints> endpointsWriter;
  }

  @Inject
  public PatroniEndpointsReconciliator(Parameters parameters) {
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.podIp = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_IP);
    this.patroniCtl = parameters.patroniCtl;
    this.endpointsFinder = parameters.endpointsFinder;
    this.endpointsWriter = parameters.endpointsWriter;
    this.lastEnpointSubset = new AtomicReference<>();
  }

  public static PatroniEndpointsReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new PatroniEndpointsReconciliator(parameters.findAny().get());
  }

  @Override
  public ReconciliationResult<Void> safeReconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    final StackGresCluster cluster = context.getCluster();
    final var patroniCtl = this.patroniCtl.instanceFor(cluster);
    if (!PatroniUtil.isPrimary(podName, patroniCtl)) {
      return new ReconciliationResult<>();
    }
    var patroniEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.readWriteName(cluster), cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalStateException("Can not find Patroni read/write endpoints "
            + PatroniUtil.readWriteName(cluster)));
    var primarySubset = new EndpointSubsetBuilder()
        .withAddresses(new EndpointAddressBuilder()
            .withIp(podIp)
            .build())
        .withPorts(PatroniUtil.getPatroniEndpointPorts(cluster))
        .build();
    if (patroniEndpoints.getSubsets() != null
        && patroniEndpoints.getSubsets().size() == 1
        && lastEnpointSubset.get() != null
        && Objects.equals(
            lastEnpointSubset.get().v1,
            primarySubset)
        && Objects.equals(
            patroniEndpoints.getSubsets().getFirst(),
            lastEnpointSubset.get().v2)) {
      return new ReconciliationResult<>();
    }
    var lastPatroniEndpoints =
        endpointsWriter.update(patroniEndpoints, currentPatroniEndpoints -> {
          currentPatroniEndpoints.setSubsets(List.of(primarySubset));
        });
    lastEnpointSubset.set(Tuple.tuple(
        primarySubset, lastPatroniEndpoints.getSubsets().getFirst()));
    LOGGER.info("Pod {} is now the primary with IP {}", podName, podIp);

    return new ReconciliationResult<>();
  }

}
