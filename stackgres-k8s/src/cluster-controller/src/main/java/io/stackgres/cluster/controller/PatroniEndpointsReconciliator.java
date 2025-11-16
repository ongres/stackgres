/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniEndpointsReconciliator extends SafeReconciliator<ClusterContext, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniEndpointsReconciliator.class);

  private final PatroniCtl patroniCtl;
  private final ResourceFinder<Endpoints> endpointsFinder;
  private final ResourceWriter<Endpoints> endpointsWriter;

  @Dependent
  public static class Parameters {
    @Inject PatroniCtl patroniCtl;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject ResourceWriter<Endpoints> endpointsWriter;
  }

  @Inject
  public PatroniEndpointsReconciliator(Parameters parameters) {
    this.patroniCtl = parameters.patroniCtl;
    this.endpointsFinder = parameters.endpointsFinder;
    this.endpointsWriter = parameters.endpointsWriter;
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
    final var primaryMember = members.stream().filter(PatroniMember::isPrimary).findFirst();
    if (patroniEndpoints.getSubsets() != null
        && !patroniEndpoints.getSubsets().isEmpty()
        && primaryMember.isEmpty()) {
      endpointsWriter.update(patroniEndpoints, currentPatroniEndpoints -> {
        if (currentPatroniEndpoints.getSubsets() != null
            && !currentPatroniEndpoints.getSubsets().isEmpty()) {
          currentPatroniEndpoints.setSubsets(null);
        }
      });
      LOGGER.info("Primary not found among members of this SGCluster: {}",
          members.stream().map(PatroniMember::getMember).collect(Collectors.joining(" ")));
    } else if (primaryMember.isPresent()) {
      var ports = PatroniUtil.getPatroniEndpointPorts(cluster);
      final String primaryHost = primaryMember.get().getHost();
      if (patroniEndpoints.getSubsets() != null
          && patroniEndpoints.getSubsets().size() == 1
              && patroniEndpoints.getSubsets().getFirst().getAddresses() != null
          && patroniEndpoints.getSubsets().getFirst().getAddresses().size() == 1
          && Objects.equals(
              patroniEndpoints.getSubsets().getFirst().getAddresses().getFirst().getIp(),
              primaryHost)
          && patroniEndpoints.getSubsets().getFirst().getPorts() != null
          && patroniEndpoints.getSubsets().getFirst().getPorts().size() == ports.size()
          && patroniEndpoints.getSubsets().getFirst().getPorts().stream()
          .allMatch(patroniEndpointPort -> ports.stream().anyMatch(
              port -> Objects.equals(patroniEndpointPort.getName(), port.getName())
              && Objects.equals(patroniEndpointPort.getProtocol(), port.getProtocol())
              && Objects.equals(patroniEndpointPort.getPort(), port.getPort())))) {
        return new ReconciliationResult<>();
      }
      endpointsWriter.update(patroniEndpoints, currentPatroniEndpoints -> {
        currentPatroniEndpoints.setSubsets(List.of(new EndpointSubsetBuilder()
            .withAddresses(new EndpointAddressBuilder()
                .withIp(primaryHost)
                .build())
            .withPorts(ports)
            .build()));
      });
      LOGGER.info("Pod {} is now the primary with IP {}",
          primaryMember.get().getMember(),
          primaryHost);
    }
    return new ReconciliationResult<>();
  }

}
