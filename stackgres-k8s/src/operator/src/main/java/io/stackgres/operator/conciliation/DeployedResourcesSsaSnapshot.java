/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.operator.conciliation.AbstractReconciliationHandler.STACKGRES_FIELD_MANAGER;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployedResourcesSsaSnapshot implements DeployedResourcesSnapshot {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeployedResourcesSsaSnapshot.class);

  private final HasMetadata generator;
  private final List<HasMetadata> ownedDeployedResources;
  private final List<HasMetadata> deployedResources;
  private final Map<ResourceKey, DeployedResource> map;
  private final ObjectMapper objectMapper;

  DeployedResourcesSsaSnapshot(
      HasMetadata generator,
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources,
      ObjectMapper objectMapper) {
    this.generator = generator;
    this.ownedDeployedResources = ownedDeployedResources;
    this.deployedResources = deployedResources;
    this.map = deployedResources.stream()
        .map(resource -> Tuple.tuple(
            ResourceKey.create(generator, resource),
            DeployedResource.create(resource, objectMapper.valueToTree(resource))))
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));
    this.objectMapper = objectMapper;
  }

  @Override
  public List<HasMetadata> ownedDeployedResources() {
    return ownedDeployedResources;
  }

  @Override
  public List<HasMetadata> deployedResources() {
    return deployedResources;
  }

  @Override
  public DeployedResource get(HasMetadata requiredResource) {
    return map.get(ResourceKey.create(generator, requiredResource));
  }

  @Override
  public Stream<HasMetadata> streamDeployed() {
    return map.values().stream()
        .map(DeployedResource::foundDeployed);
  }

  @Override
  public boolean isDeployed(HasMetadata requiredResource) {
    return map.containsKey(ResourceKey.create(generator, requiredResource));
  }

  @Override
  public boolean isChanged(HasMetadata requiredResource, DeployedResource deployedResource) {
    ObjectNode requiredResourceNode = objectMapper.valueToTree(requiredResource);
    ObjectNode deployedResourceNode = deployedResource.foundDeployedNode();
    if (requiredResource instanceof Secret requiredSecret) {
      if (requiredSecret.getStringData() != null
          && !requiredSecret.getStringData().isEmpty()) {
        requiredResourceNode.set(
            "data",
            objectMapper.valueToTree(
                ResourceUtil.encodeSecret(
                    requiredSecret.getStringData())));
      }
    }
    if (deployedResource.foundDeployed() instanceof Secret deployedSecret) {
      if (deployedSecret.getData() != null
          && !deployedSecret.getData().isEmpty()) {
        deployedResourceNode.set(
            "stringData",
            objectMapper.valueToTree(
                ResourceUtil.decodeSecret(
                    deployedSecret.getData())));
      }
    }
    ArrayNode managedFields = Optional.of(deployedResourceNode)
        .map(resource -> resource.get("metadata"))
        .map(metadata -> metadata.get("managedFields"))
        .filter(ArrayNode.class::isInstance)
        .map(ArrayNode.class::cast)
        .orElse(null);
    if (managedFields == null) {
      LOGGER.warn("Managed fields array was not found");
      return true;
    }
    for (JsonNode managedFieldsEntry : managedFields) {
      JsonNode fieldsType = managedFieldsEntry.get("fieldsType");
      if (fieldsType == null
          || !Objects.equals(
              fieldsType.asText(),
              "FieldsV1")) {
        LOGGER.warn("Managed fields has missing or unkown type ({})", fieldsType);
        return true;
      }
      JsonNode manager = managedFieldsEntry.get("manager");
      JsonNode subresource = managedFieldsEntry.get("subresource");
      JsonNode fieldsV1 = managedFieldsEntry.get("fieldsV1");
      if (subresource == null
          && manager != null
          && Objects.equals(
              manager.asText(),
              STACKGRES_FIELD_MANAGER)) {
        if (anyManagedFieldsDiff(fieldsV1, requiredResourceNode, deployedResourceNode)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean anyManagedFieldsDiff(JsonNode fieldsV1, JsonNode required, JsonNode deployed) {
    if (fieldsV1.properties().isEmpty()) {
      if (required == null && deployed == null) {
        return false;
      }
      if (required != null && deployed != null) {
        if (required.isArray() || required.isObject()
            || deployed.isArray() || deployed.isObject()) {
          return false;
        }
        if (!Objects.equals(required, deployed)) {
          return true;
        }
        return false;
      }
      if (required == null && deployed != null) {
        return true;
      }
      if (required != null && deployed == null) {
        return true;
      }
      return false;
    }
    if (fieldsV1.propertyStream()
        .anyMatch(property -> anyManagedFieldsDiff(
            property.getValue(),
            extractManagedFieldsKey(property.getKey(), required),
            extractManagedFieldsKey(property.getKey(), deployed)))) {
      return true;
    }
    return false;
  }

  private JsonNode extractManagedFieldsKey(String key, JsonNode resource) {
    if (key.startsWith("f:")) {
      if (resource == null) {
        return null;
      }
      return resource.get(key.substring(2));
    } else if (key.startsWith("k:")) {
      if (resource == null) {
        return null;
      }
      if (!(resource instanceof ArrayNode resourceArray)) {
        LOGGER.warn("Managed fields key {} but resource was not an array: {}", key, resource);
        return null;
      }
      final JsonNode keyNode;
      try {
        keyNode = objectMapper.readTree(key.substring(2));
      } catch (JsonProcessingException ex) {
        LOGGER.warn("Managed fields key {} can not be parsed", key, ex);
        return null;
      }
      Set<Entry<String, JsonNode>> keyObjectNode = keyNode.properties();
      if (keyObjectNode.size() < 1) {
        LOGGER.warn("Managed fields key {} has no elements", key);
        return null;
      }
      Optional<ObjectNode> valueNodeFound = resourceArray.valueStream()
          .filter(ObjectNode.class::isInstance)
          .map(ObjectNode.class::cast)
          .filter(entry -> keyObjectNode.stream()
              .allMatch(keyObjectNodeEntry -> Objects.equals(
                  entry.get(keyObjectNodeEntry.getKey()),
                  keyObjectNodeEntry.getValue())))
          .findFirst();
      if (valueNodeFound.isPresent()) {
        return valueNodeFound.get();
      }
      return null;
    } else if (Objects.equals(key, ".")) {
      return null;
    }
    LOGGER.warn("Managed fields key {} has an unknown prefix", key);
    return null;
  }

}
