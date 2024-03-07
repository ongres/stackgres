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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import io.stackgres.operatorframework.resource.ResourceUtil;
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
    final var patroniEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.readWriteName(cluster), cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalStateException("Can not find Patroni read/write endpoints "
            + PatroniUtil.readWriteName(cluster)));
    final var patroniCtl = this.patroniCtl.instanceFor(cluster);
    final var members = patroniCtl.list();
    if (members.stream()
        .filter(PatroniMember::isPrimary)
        .map(PatroniMember::getMember)
        .noneMatch(podName::equals)) {
      final Pattern nameWithIndexPattern =
          ResourceUtil.getNameWithIndexPattern(cluster.getMetadata().getName());
      if (patroniEndpoints.getSubsets() != null
          && !patroniEndpoints.getSubsets().isEmpty()
          && members.stream()
          .filter(PatroniMember::isPrimary)
          .map(PatroniMember::getMember)
          .map(nameWithIndexPattern::matcher)
          .noneMatch(Matcher::find)) {
        LOGGER.info("Primary not found among members of this SGCluster: {}",
            members.stream().map(PatroniMember::getMember).collect(Collectors.joining(" ")));
        endpointsWriter.update(patroniEndpoints, currentPatroniEndpoints -> {
          if (currentPatroniEndpoints.getSubsets() != null
              && !currentPatroniEndpoints.getSubsets().isEmpty()) {
            currentPatroniEndpoints.setSubsets(null);
          }
        });
        lastEnpointSubset.set(null);
      } else if (patroniEndpoints.getSubsets() != null
          && patroniEndpoints.getSubsets().stream().anyMatch(subset -> subset.getAddresses() != null
          && subset.getAddresses().stream().anyMatch(address -> address.getIp().equals(podIp)))) {
        LOGGER.info("Pod {} with IP {} is no longer the primary", podName, podIp);
        endpointsWriter.update(patroniEndpoints, currentPatroniEndpoints -> {
          if (currentPatroniEndpoints.getSubsets() != null
              && currentPatroniEndpoints.getSubsets().stream().anyMatch(subset -> subset.getAddresses() != null
              && subset.getAddresses().stream().anyMatch(address -> address.getIp().equals(podIp)))) {
            currentPatroniEndpoints.setSubsets(
                currentPatroniEndpoints.getSubsets().stream()
                .filter(subset -> subset.getAddresses().stream()
                    .noneMatch(address -> address.getIp().equals(podIp)))
                .toList());
          }
        });
        lastEnpointSubset.set(null);
      }
      return new ReconciliationResult<>();
    }
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
            primarySubset,
            lastEnpointSubset.get().v1)
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
