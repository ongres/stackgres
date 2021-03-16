/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.handler;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.patroni.factory.PatroniEndpoints;
import io.stackgres.operator.patroni.factory.PatroniServices;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.visitor.PairVisitor;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniConfigEndpointsHandler extends AbstractClusterResourceHandler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PatroniConfigEndpointsHandler.class);

  private final ObjectMapper objectMapper;

  private final PatroniServices patroniServices;

  @Inject
  public PatroniConfigEndpointsHandler(ObjectMapperProvider objectMapperProvider,
                                       PatroniServices patroniServices) {
    super();
    this.objectMapper = objectMapperProvider.objectMapper();
    this.patroniServices = patroniServices;
  }

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof Endpoints
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            patroniServices.configName(context));
  }

  @Override
  public boolean equals(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new EndpointsVisitor<>(context),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new EndpointsVisitor<>(context),
        existingResource, requiredResource);
  }

  private class EndpointsVisitor<T>
      extends ResourcePairVisitor<T, StackGresClusterContext> {

    public EndpointsVisitor(StackGresClusterContext context) {
      super(context);
    }

    @Override
    public PairVisitor<HasMetadata, T> visit(
        PairVisitor<HasMetadata, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(HasMetadata::getApiVersion, HasMetadata::setApiVersion)
          .visit(HasMetadata::getKind)
          .visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata,
              this::visitEndpointsMetadata)
          .lastVisit(this::visitEndpoints);
    }

    public PairVisitor<ObjectMeta, T> visitEndpointsMetadata(
        PairVisitor<ObjectMeta, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(ObjectMeta::getClusterName, ObjectMeta::setClusterName)
          .visit(ObjectMeta::getDeletionGracePeriodSeconds,
              ObjectMeta::setDeletionGracePeriodSeconds)
          .visit(ObjectMeta::getName, ObjectMeta::setName)
          .visit(ObjectMeta::getNamespace, ObjectMeta::setNamespace)
          .visitList(ObjectMeta::getFinalizers, ObjectMeta::setFinalizers)
          .visitMap(ObjectMeta::getAdditionalProperties,
              additionalPropertiesSetter(
                  ObjectMeta::getAdditionalProperties,
                  ObjectMeta::setAdditionalProperty))
          .visitMapTransformed(ObjectMeta::getAnnotations, ObjectMeta::setAnnotations,
              this::tranformExistingEndpointsAnnotations,
              this::tranformRequiredEndpointsAnnotations,
              () -> new HashMap<>())
          .visitMap(ObjectMeta::getLabels, ObjectMeta::setLabels);
    }

    public Map.Entry<String, String> tranformExistingEndpointsAnnotations(
        Map.Entry<String, String> leftEntry, Map.Entry<String, String> rightEntry) {
      if (leftEntry != null
          && leftEntry.getKey().equals(PatroniEndpoints.PATRONI_CONFIG_KEY)) {
        try {
          JsonNode existingPatroniConfig = readOrderedTree(leftEntry.getValue());
          String existingRightEntryValue = objectMapper.writeValueAsString(existingPatroniConfig);
          return new SimpleEntry<>(rightEntry.getKey(), existingRightEntryValue);
        } catch (IOException ex) {
          LOGGER.warn("Error reading existing patroni configuration", ex);
        }
      }

      return leftEntry;
    }

    public Map.Entry<String, String> tranformRequiredEndpointsAnnotations(
        Map.Entry<String, String> leftEntry, Map.Entry<String, String> rightEntry) {
      if ((rightEntry != null //NOPMD
          && rightEntry.getKey().equals(PatroniEndpoints.PATRONI_CONFIG_KEY))
          || (leftEntry != null //NOPMD
          && leftEntry.getKey().equals(PatroniEndpoints.PATRONI_CONFIG_KEY))) {
        final JsonNode requiredPatroniConfig;
        try {
          requiredPatroniConfig = readOrderedTree(rightEntry.getValue());
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        try {
          String requiredPatroniConfigValue = objectMapper.writeValueAsString(
              requiredPatroniConfig);
          return new SimpleEntry<>(rightEntry.getKey(), requiredPatroniConfigValue);
        } catch (JsonProcessingException ex) {
          throw new RuntimeException(ex);
        }
      }

      if (rightEntry == null) {
        return leftEntry;
      }

      return rightEntry;
    }

    private JsonNode readOrderedTree(String value) throws IOException {
      return orderTree(objectMapper.readTree(value));
    }

    private JsonNode orderTree(JsonNode node) {
      if (node instanceof ObjectNode) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        Seq.seq(ObjectNode.class.cast(node).fieldNames())
            .sorted()
            .forEach(fieldName -> objectNode.set(fieldName, orderTree(node.get(fieldName))));
        return objectNode;
      } else if (node instanceof ArrayNode) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        ArrayNode.class.cast(node)
            .forEach(elementNode -> arrayNode.add(orderTree(elementNode)));
        return arrayNode;
      }

      return node;
    }
  }
}
