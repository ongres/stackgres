/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DeployedResourcesCache {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeployedResourcesCache.class);

  private final Cache<ResourceKey, DeployedResource> cache;
  private final ObjectMapper objectMapper;

  @Inject
  public DeployedResourcesCache(
      OperatorPropertyContext propertyContext,
      ObjectMapper objectMapper) {
    var cacheBuilder = Caffeine.newBuilder();
    propertyContext.get(
        OperatorProperty.RECONCILIATION_CACHE_EXPIRATION)
        .map(Integer::valueOf)
        .ifPresent(duration -> cacheBuilder.expireAfterWrite(Duration.ofSeconds(duration)));
    propertyContext.get(
        OperatorProperty.RECONCILIATION_CACHE_SIZE)
        .map(Integer::valueOf)
        .ifPresent(size -> cacheBuilder.maximumSize(size));
    this.cache = cacheBuilder.build();
    this.objectMapper = objectMapper;
  }

  public void put(
      HasMetadata generator,
      HasMetadata requiredResource,
      HasMetadata deployedResource) {
    final ResourceKey key = ResourceKey.create(generator, requiredResource);
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
    cache.put(key,
        DeployedResource.create(
            requiredResource,
            deployedResource,
            toComparableDeployedNode(requiredResource, deployedResource)));
  }

  public void remove(
      HasMetadata generator,
      HasMetadata deletedResource) {
    final ResourceKey key = ResourceKey.create(generator, deletedResource);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Remove {} required resource {} {}.{}",
          cache.getIfPresent(key) == null ? "new" : "existing",
          deletedResource.getKind(),
          deletedResource.getMetadata().getNamespace(),
          deletedResource.getMetadata().getName());
    }
    cache.invalidate(key);
  }

  public void removeAll(
      HasMetadata generator) {
    cache.asMap().keySet().stream()
        .filter(key -> key.isGeneratedBy(generator))
        .forEach(cache::invalidate);
  }

  public DeployedResource get(
      HasMetadata generator,
      HasMetadata requiredResource) {
    return cache.getIfPresent(ResourceKey.create(generator, requiredResource));
  }

  public Stream<DeployedResource> stream() {
    return cache.asMap().values().stream();
  }

  public DeployedResourcesSnapshot createDeployedResourcesSnapshot(
      HasMetadata generator,
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources) {
    var deployedResourcesMap = new HashMap<>(cache.asMap());
    deployedResources.stream()
        .forEach(resource -> putOrUpdateLatest(generator, resource, deployedResourcesMap));
    putAll(deployedResourcesMap);
    return new DeployedResourcesSnapshot(
        generator, ownedDeployedResources, deployedResources, deployedResourcesMap);
  }

  private void putOrUpdateLatest(
      HasMetadata generator,
      HasMetadata foundDeployedResource,
      Map<ResourceKey, DeployedResource> deployedResourceMap) {
    ResourceKey key = ResourceKey.create(generator, foundDeployedResource);
    DeployedResource deployedResource = deployedResourceMap.get(key);
    if (deployedResource != null) {
      if (Objects.equals(
          deployedResource.foundDeployed().getMetadata().getResourceVersion(),
          foundDeployedResource.getMetadata().getResourceVersion())) {
        return;
      }
      if (deployedResource.required().isPresent()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updated previously required resource {} {}.{}",
              foundDeployedResource.getKind(),
              foundDeployedResource.getMetadata().getNamespace(),
              foundDeployedResource.getMetadata().getName());
        }
        HasMetadata requiredResource = deployedResource.required().get();
        deployedResourceMap.put(key,
            DeployedResource.create(
                requiredResource,
                deployedResource.deployed(),
                deployedResource.deployedNode(),
                foundDeployedResource,
                toComparableDeployedNode(requiredResource, foundDeployedResource)));
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updated already found resource {} {}.{}",
              foundDeployedResource.getKind(),
              foundDeployedResource.getMetadata().getNamespace(),
              foundDeployedResource.getMetadata().getName());
        }
        deployedResourceMap.put(key,
            DeployedResource.create(
                deployedResource.deployed(),
                deployedResource.deployedNode(),
                foundDeployedResource,
                null));
      }
    } else {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Found resource {} {}.{}",
            foundDeployedResource.getKind(),
            foundDeployedResource.getMetadata().getNamespace(),
            foundDeployedResource.getMetadata().getName());
      }
      deployedResourceMap.put(key,
          DeployedResource.create(
              foundDeployedResource,
              null));
    }
  }

  private void putAll(Map<ResourceKey, DeployedResource> deployedResourcesMap) {
    cache.putAll(deployedResourcesMap);
  }

  public void removeWithLabelsNotIn(
      HasMetadata generator,
      Map<String, String> genericLabels,
      List<HasMetadata> deployedResources) {
    Set<ResourceKey> deployedKeys = deployedResources
        .stream()
        .map(resource -> ResourceKey.create(generator, resource))
        .collect(Collectors.toSet());
    cache.asMap().entrySet().stream()
        .filter(e -> e.getKey().isGeneratedBy(generator))
        .map(e -> Tuple.tuple(
            e.getKey(),
            Optional.ofNullable(e.getValue().foundDeployed().getMetadata().getLabels())
            .orElse(Map.of())))
        .filter(t -> genericLabels.entrySet().stream()
            .allMatch(genericLabel -> t.v2.entrySet().stream().anyMatch(genericLabel::equals)))
        .map(Tuple2::v1)
        .toList()
        .stream()
        .filter(Predicate.not(deployedKeys::contains))
        .forEach(this::invalidateKey);
  }

  private void invalidateKey(ResourceKey key) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Invalidating {} resource {} {}.{}",
          cache.getIfPresent(key) == null ? "new" : "existing",
          key.kind(),
          key.namespace(),
          key.name());
    }
    cache.invalidate(key);
  }

  private ObjectNode toComparableDeployedNode(
      HasMetadata requiredResource,
      HasMetadata deployedResource) {
    ObjectNode deployedNode = objectMapper.valueToTree(deployedResource);
    var deployedMetadata = deployedNode.get("metadata");
    if (deployedMetadata instanceof NullNode) {
      deployedNode.remove("metadata");
    } else if (deployedMetadata != null) {
      ObjectNode comparableDeployedMetadata = objectMapper.createObjectNode();
      JsonNode deployedAnnotations = deployedMetadata.get("annotations");
      if (deployedAnnotations instanceof ObjectNode deployedAnnotationsObject) {
        Map<String, String> requiredResourceAnnotations = Optional
            .ofNullable(requiredResource.getMetadata().getAnnotations())
            .orElse(Map.of());
        Seq.seq(deployedAnnotationsObject.fieldNames()).toList().stream()
            .filter(Predicate.not(requiredResourceAnnotations::containsKey))
            .forEach(deployedAnnotationsObject::remove);
      }
      if (deployedAnnotations == null || deployedAnnotations instanceof NullNode) {
        deployedAnnotations = objectMapper.createObjectNode();
      }
      comparableDeployedMetadata.set("annotations", deployedAnnotations);
      JsonNode deployedLabels = deployedMetadata.get("labels");
      if (deployedLabels instanceof ObjectNode deployedLabelsObject) {
        Map<String, String> requiredResourceLabels = Optional
            .ofNullable(requiredResource.getMetadata().getLabels())
            .orElse(Map.of());
        Seq.seq(deployedLabelsObject.fieldNames()).toList().stream()
            .filter(Predicate.not(requiredResourceLabels::containsKey))
            .forEach(deployedLabelsObject::remove);
      }
      if (deployedLabels == null || deployedLabels instanceof NullNode) {
        deployedLabels = objectMapper.createObjectNode();
      }
      comparableDeployedMetadata.set("labels", deployedLabels);
      comparableDeployedMetadata.set("ownerReferences", deployedMetadata.get("ownerReferences"));
      deployedNode.set("metadata", comparableDeployedMetadata);
    }
    if (deployedNode.has("status")) {
      deployedNode.remove("status");
    }
    // Native image requires this. It is not clear but seems subsets are not deserialized when
    // returned after patching
    if (requiredResource instanceof Endpoints requiredEndpoints
        && (requiredEndpoints.getSubsets() == null
        || requiredEndpoints.getSubsets().isEmpty())) {
      deployedNode.remove("subsets");
    }
    return deployedNode;
  }

}
