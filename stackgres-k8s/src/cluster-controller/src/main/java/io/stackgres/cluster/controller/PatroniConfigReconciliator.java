/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniConfigReconciliator extends SafeReconciliator<ClusterContext, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniConfigReconciliator.class);

  private final PatroniCtl patroniCtl;
  private final ResourceFinder<Endpoints> endpointsFinder;
  private final ResourceWriter<Endpoints> endpointsWriter;
  private final ObjectMapper objectMapper;

  @Dependent
  public static class Parameters {
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject PatroniCtl patroniCtl;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject ResourceWriter<Endpoints> endpointsWriter;
    @Inject ObjectMapper objectMapper;
  }

  @Inject
  public PatroniConfigReconciliator(Parameters parameters) {
    this.patroniCtl = parameters.patroniCtl;
    this.endpointsFinder = parameters.endpointsFinder;
    this.endpointsWriter = parameters.endpointsWriter;
    this.objectMapper = parameters.objectMapper;
  }

  public static PatroniConfigReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new PatroniConfigReconciliator(parameters.findAny().get());
  }

  @Override
  public ReconciliationResult<Void> safeReconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    final StackGresCluster cluster = context.getCluster();
    final var patroniCtl = this.patroniCtl.instanceFor(cluster);
    var patroniConfigEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.configName(cluster), cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalStateException("Can not find Patroni config endpoints "
            + PatroniUtil.configName(cluster)));
    var patroniConfig = Optional.ofNullable(patroniConfigEndpoints.getMetadata().getAnnotations())
        .map(annotations -> annotations.get(PatroniUtil.CONFIG_KEY))
        .map(Unchecked.function(objectMapper::readTree))
        .map(ObjectNode.class::cast);
    if (patroniConfig.isEmpty()) {
      return new ReconciliationResult<>();
    }
    var patroniConfigFound = patroniCtl.showConfigJson();
    var mergedPatroniConfig = mergeConfig(patroniConfigFound, patroniConfig.get());
    patroniCtl.editConfigJson(mergedPatroniConfig);
    endpointsWriter.update(patroniConfigEndpoints, Unchecked.consumer(
        currentPatroniConfigEndpoints -> {
          currentPatroniConfigEndpoints.getMetadata().setAnnotations(Stream.concat(
              Optional.ofNullable(patroniConfigEndpoints.getMetadata().getAnnotations())
              .orElse(Map.of())
              .entrySet()
              .stream()
              .filter(entry -> !entry.getKey().equals(PatroniUtil.CONFIG_KEY)),
              Stream.of(Map.entry(
                  PatroniUtil.CONFIG_KEY,
                  mergedPatroniConfig.toString())))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }));
    LOGGER.info("Patroni config was updated");

    return new ReconciliationResult<>();
  }

  private ObjectNode mergeConfig(ObjectNode foundConfig, ObjectNode value) {
    try {
      JsonNode previousConfig = objectMapper.valueToTree(
          objectMapper.readValue(foundConfig.toString(), PatroniConfig.class));
      previousConfig.fieldNames().forEachRemaining(foundConfig::remove);
      JsonNode updatedConfig = objectMapper.readerForUpdating(foundConfig)
          .readTree(value.toString());
      return (ObjectNode) updatedConfig;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
