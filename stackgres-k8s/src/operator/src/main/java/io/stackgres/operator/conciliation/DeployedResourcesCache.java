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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DeployedResourcesCache {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeployedResourcesCache.class);

  private final Cache<ResourceKey, DeployedResourceValue> cache;
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

  public void put(HasMetadata requiredResource, HasMetadata deployedResource) {
    final ResourceKey key = ResourceKey.create(requiredResource);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} required resource {} {}.{}",
          cache.getIfPresent(key) == null ? "Put new" : "Update existing",
          deployedResource.getKind(),
          deployedResource.getMetadata().getNamespace(),
          deployedResource.getMetadata().getName());
    }
    cache.put(key,
        DeployedResourceValue.create(
            requiredResource,
            deployedResource,
            toComparableDeployedNode(requiredResource, deployedResource)));
  }

  public void remove(HasMetadata deletedResource) {
    final ResourceKey key = ResourceKey.create(deletedResource);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Remove {} required resource {} {}.{}",
          cache.getIfPresent(key) == null ? "new" : "existing",
          deletedResource.getKind(),
          deletedResource.getMetadata().getNamespace(),
          deletedResource.getMetadata().getName());
    }
    cache.invalidate(key);
  }

  public DeployedResourceValue get(HasMetadata requiredResource) {
    return cache.getIfPresent(ResourceKey.create(requiredResource));
  }

  public Stream<DeployedResourceValue> stream() {
    return cache.asMap().values().stream();
  }

  public DeployedResourcesSnapshot createDeployedResourcesSnapshot(
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources) {
    var deployedResourcesMap = new HashMap<>(cache.asMap());
    deployedResources.stream()
        .map(resource -> Tuple.tuple(resource, deployedResourcesMap))
        .forEach(t -> putOrUpdateLatest(t.v1, t.v2));
    putAll(deployedResourcesMap);
    return new DeployedResourcesSnapshot(
        ownedDeployedResources, deployedResources, deployedResourcesMap);
  }

  private void putOrUpdateLatest(
      HasMetadata latestDeployedResource,
      Map<ResourceKey, DeployedResourceValue> deployedResourceMap) {
    ResourceKey key = ResourceKey.create(latestDeployedResource);
    DeployedResourceValue deployedResourceValue = deployedResourceMap.get(key);
    if (deployedResourceValue != null) {
      if (Objects.equals(
          deployedResourceValue.latestDeployed().getMetadata().getResourceVersion(),
          latestDeployedResource.getMetadata().getResourceVersion())) {
        return;
      }
      if (deployedResourceValue.required().isPresent()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updated previously required resource {} {}.{}",
              latestDeployedResource.getKind(),
              latestDeployedResource.getMetadata().getNamespace(),
              latestDeployedResource.getMetadata().getName());
        }
        HasMetadata requiredResource = deployedResourceValue.required().get();
        deployedResourceMap.put(key,
            DeployedResourceValue.create(
                requiredResource,
                deployedResourceValue.deployed(),
                deployedResourceValue.deployedNode(),
                latestDeployedResource,
                toComparableDeployedNode(requiredResource, latestDeployedResource)));
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updated already found resource {} {}.{}",
              latestDeployedResource.getKind(),
              latestDeployedResource.getMetadata().getNamespace(),
              latestDeployedResource.getMetadata().getName());
        }
        deployedResourceMap.put(key,
            DeployedResourceValue.create(
                deployedResourceValue.deployed(),
                deployedResourceValue.deployedNode(),
                latestDeployedResource,
                null));
      }
    } else {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Found resource {} {}.{}",
            latestDeployedResource.getKind(),
            latestDeployedResource.getMetadata().getNamespace(),
            latestDeployedResource.getMetadata().getName());
      }
      deployedResourceMap.put(key,
          DeployedResourceValue.create(
              latestDeployedResource,
              null));
    }
  }

  private void putAll(Map<ResourceKey, DeployedResourceValue> deployedResourcesMap) {
    cache.putAll(deployedResourcesMap);
  }

  public void removeWithLabelsNotIn(
      Map<String, String> genericLabels,
      List<HasMetadata> deployedResources) {
    Set<ResourceKey> deployedKeys = deployedResources
        .stream()
        .map(ResourceKey::create)
        .collect(Collectors.toSet());
    cache.asMap().entrySet().stream()
        .map(e -> Tuple.tuple(
            e.getKey(),
            Optional.ofNullable(e.getValue().latestDeployed().getMetadata().getLabels())
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
        Seq.seq(deployedAnnotationsObject.fieldNames()).toList().stream()
            .filter(Predicate.not(requiredResource.getMetadata().getAnnotations()::containsKey))
            .forEach(deployedAnnotationsObject::remove);
      }
      if (deployedAnnotations == null || deployedAnnotations instanceof NullNode) {
        deployedAnnotations = objectMapper.createObjectNode();
      }
      comparableDeployedMetadata.set("annotations", deployedAnnotations);
      JsonNode deployedLabels = deployedMetadata.get("labels");
      if (deployedLabels instanceof ObjectNode deployedLabelsObject) {
        Seq.seq(deployedLabelsObject.fieldNames()).toList().stream()
            .filter(Predicate.not(requiredResource.getMetadata().getLabels()::containsKey))
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
    if (requiredResource instanceof Endpoints requiredEndpoints
        && (requiredEndpoints.getSubsets() == null
        || requiredEndpoints.getSubsets().isEmpty())) {
      deployedNode.remove("subsets");
    }
    return deployedNode;
  }

}
