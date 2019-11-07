/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractResourceHandler;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniConfigEndpointsHandler extends AbstractResourceHandler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PatroniConfigEndpointsHandler.class);

  private final ObjectMapper objectMapper;

  @Inject
  public PatroniConfigEndpointsHandler(ObjectMapperProvider objectMapperProvider) {
    super();
    this.objectMapper = objectMapperProvider.objectMapper();
  }

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource.getKind().equals("Endpoints")
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + PatroniServices.CONFIG_SERVICE);
  }

  @Override
  public boolean equals(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new EndpointsVisitor<>(), existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new EndpointsVisitor<>(), existingResource, requiredResource);
  }

  private class EndpointsVisitor<T> extends ResourcePairVisitor<T> {

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
          .visitMap(ObjectMeta::getAdditionalProperties)
          .visitMapTransformed(ObjectMeta::getAnnotations, ObjectMeta::setAnnotations,
              this::tranformExistingEndpointsAnnotations,
              this::tranformRequiredEndpointsAnnotations)
          .visitMap(ObjectMeta::getLabels, ObjectMeta::setLabels);
    }

    public Map.Entry<String, String> tranformExistingEndpointsAnnotations(
        Map.Entry<String, String> leftEntry, Map.Entry<String, String> rightEntry) {
      if (leftEntry != null
          && leftEntry.getKey().equals(PatroniConfigEndpoints.PATRONI_CONFIG_KEY)) {
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
          && rightEntry.getKey().equals(PatroniConfigEndpoints.PATRONI_CONFIG_KEY))
          || (leftEntry != null //NOPMD
          && leftEntry.getKey().equals(PatroniConfigEndpoints.PATRONI_CONFIG_KEY))) {
        final JsonNode requiredPatroniConfig;
        try {
          requiredPatroniConfig = readOrderedTree(rightEntry.getValue());
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        JsonNode existingPatroniConfig;
        try {
          existingPatroniConfig = readOrderedTree(leftEntry.getValue());
        } catch (IOException ex) {
          LOGGER.warn("Error reading existing patroni configuration", ex);
          existingPatroniConfig = requiredPatroniConfig;
        }
        try {
          final JsonNode modifiedPatroniConfig = existingPatroniConfig;
          ObjectNode.class.cast(modifiedPatroniConfig)
            .set("postgresql", requiredPatroniConfig.get("postgresql"));
          String modifiedLeftEntryValue = objectMapper.writeValueAsString(modifiedPatroniConfig);
          return new SimpleEntry<>(rightEntry.getKey(), modifiedLeftEntryValue);
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
