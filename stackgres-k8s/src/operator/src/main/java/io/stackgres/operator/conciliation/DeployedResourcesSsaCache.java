/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.operator.conciliation.AbstractReconciliationHandler.STACKGRES_FIELD_MANAGER;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployedResourcesSsaCache implements DeployedResourcesCache {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeployedResourcesSsaCache.class);

  private final ObjectMapper objectMapper;
  private final Cache<ResourceKindKey, Map<String, JsonNode>> cache;

  @Inject
  public DeployedResourcesSsaCache(
      OperatorPropertyContext propertyContext,
      ObjectMapper objectMapper) {
    var cacheBuilder = Caffeine.newBuilder();
    this.objectMapper = objectMapper;
    propertyContext.get(
        OperatorProperty.RECONCILIATION_CACHE_EXPIRATION)
        .map(Integer::valueOf)
        .ifPresent(duration -> cacheBuilder.expireAfterWrite(Duration.ofSeconds(duration)));
    propertyContext.get(
        OperatorProperty.RECONCILIATION_CACHE_SIZE)
        .map(Integer::valueOf)
        .ifPresent(size -> cacheBuilder.maximumSize(size));
    this.cache = cacheBuilder.build();
  }

  public void put(
      HasMetadata generator,
      HasMetadata requiredResource,
      HasMetadata deployedResource) {
    final ResourceKindKey key = ResourceKindKey.create(requiredResource);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} required resource {} {}.{}",
          cache.getIfPresent(key) == null ? "Put new" : "Update existing",
          deployedResource.getKind(),
          deployedResource.getMetadata().getNamespace(),
          deployedResource.getMetadata().getName());
    }
    if (requiredResource.getMetadata() != null
        && requiredResource.getMetadata().getManagedFields() != null
        && requiredResource.getMetadata().getManagedFields().isEmpty()) {
      requiredResource.getMetadata().setManagedFields(null);
    }
    ObjectNode requiredResourceNode = objectMapper.valueToTree(requiredResource);
    ObjectNode deployedResourceNode = objectMapper.valueToTree(deployedResource);
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
    if (deployedResource instanceof Secret deployedSecret) {
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
      return;
    }
    Map<String, JsonNode> defaultsMap = new HashMap<>(
        Optional.ofNullable(cache.getIfPresent(key))
        .orElse(Map.of()));
    for (JsonNode managedFieldsEntry : managedFields) {
      JsonNode fieldsType = managedFieldsEntry.get("fieldsType");
      if (fieldsType == null
          || !Objects.equals(
              fieldsType.asText(),
              "FieldsV1")) {
        LOGGER.warn("Managed fields has missing or unkown type ({})", fieldsType);
        return;
      }
      JsonNode manager = managedFieldsEntry.get("manager");
      JsonNode subresource = managedFieldsEntry.get("subresource");
      JsonNode fieldsV1 = managedFieldsEntry.get("fieldsV1");
      if (subresource == null
          && manager != null
          && Objects.equals(
              manager.asText(),
              STACKGRES_FIELD_MANAGER)) {
        defaultsMap.putAll(getManagedFieldsDefaults("", fieldsV1, requiredResourceNode, deployedResourceNode).stream()
            .collect(Collectors.groupingBy(Tuple2::v1))
            .entrySet()
            .stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().getFirst().v2))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
      }
    }
    cache.put(key, defaultsMap);
  }

  public void remove(
      HasMetadata generator,
      HasMetadata deletedResource) {
  }

  public void removeAll(HasMetadata generator) {
  }

  public DeployedResource get(
      HasMetadata generator,
      HasMetadata requiredResource) {
    return null;
  }

  public DeployedResourcesSnapshot createDeployedResourcesSnapshot(
      HasMetadata generator,
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources) {
    return new DeployedResourcesSsaSnapshot(
        generator, ownedDeployedResources, deployedResources, new HashMap<>(cache.asMap()), this.objectMapper);
  }

  public void removeWithLabelsNotIn(
      HasMetadata generator,
      Map<String, String> genericLabels,
      List<HasMetadata> deployedResources) {
  }

  private List<Tuple2<String, JsonNode>> getManagedFieldsDefaults(String path, JsonNode fieldsV1, JsonNode required, JsonNode deployed) {
    if (fieldsV1.properties().isEmpty()) {
      if (required == null && deployed == null) {
        return List.of();
      }
      if (required != null && deployed != null) {
        if (required.isArray() || required.isObject()
            || deployed.isArray() || deployed.isObject()) {
          return List.of();
        }
        if (!Objects.equals(required, deployed)) {
          return List.of();
        }
        return List.of();
      }
      if (required == null && deployed != null) {
        return List.of(Tuple.tuple(path, deployed));
      }
      if (required != null && deployed == null) {
        return List.of(Tuple.tuple(path, required));
      }
      return List.of();
    }
    return fieldsV1.propertyStream()
        .flatMap(property -> getManagedFieldsDefaults(
            extractManagedFieldsPath(path, property.getKey()),
            property.getValue(),
            extractManagedFieldsKey(property.getKey(), required),
            extractManagedFieldsKey(property.getKey(), deployed)).stream())
        .toList();
  }

  private String extractManagedFieldsPath(String path, String key) {
    if (key.startsWith("f:")) {
      return path + "/" + key.substring(2);
    } else if (key.startsWith("k:")) {
      return path;
    } else if (Objects.equals(key, ".")) {
      return path;
    }
    LOGGER.warn("Managed fields key {} has an unknown prefix", key);
    return path;
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
