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

  // Default cap sized to cover ~10 SGShardedCluster deployments with ~50 resources per shard
  // across ~32 shards (~16k entries) with headroom; bounded so operator memory cannot grow
  // unboundedly when the env-var overrides are not set. User-supplied properties override.
  private static final int DEFAULT_MAX_SIZE = 100_000;
  private static final Duration DEFAULT_EXPIRE_AFTER_ACCESS = Duration.ofHours(1);

  private final Cache<ResourceKey, DeployedResource> cache;
  private final ObjectMapper objectMapper;

  @Inject
  public DeployedResourcesCache(
      OperatorPropertyContext propertyContext,
      ObjectMapper objectMapper) {
    var cacheBuilder = Caffeine.newBuilder();
    Optional<Integer> configuredExpiration = propertyContext.get(
        OperatorProperty.RECONCILIATION_CACHE_EXPIRATION).map(Integer::valueOf);
    Optional<Integer> configuredSize = propertyContext.get(
        OperatorProperty.RECONCILIATION_CACHE_SIZE).map(Integer::valueOf);
    // Apply defaults so the cache can never grow unbounded when the env-var overrides are not
    // set; user-supplied properties override the defaults.
    cacheBuilder.maximumSize(configuredSize.orElse(DEFAULT_MAX_SIZE));
    if (configuredExpiration.isPresent()) {
      cacheBuilder.expireAfterWrite(Duration.ofSeconds(configuredExpiration.get()));
    } else {
      cacheBuilder.expireAfterAccess(DEFAULT_EXPIRE_AFTER_ACCESS);
    }
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
    // Only copy entries belonging to this generator, not the entire cache. Previously the
    // snapshot copy (and subsequent putAll) was O(total cache size) per reconcile and caused
    // significant cross-CR allocation coupling.
    Map<ResourceKey, DeployedResource> deployedResourcesMap = new HashMap<>();
    cache.asMap().forEach((k, v) -> {
      if (k.isGeneratedBy(generator)) {
        deployedResourcesMap.put(k, v);
      }
    });
    // Track only entries that were actually created/updated so we don't re-put unchanged
    // entries (which would refresh expireAfterWrite for every entry of this generator and
    // generate unnecessary write traffic on the shared cache).
    Map<ResourceKey, DeployedResource> modified = new HashMap<>();
    deployedResources.stream()
        .forEach(resource ->
            putOrUpdateLatest(generator, resource, deployedResourcesMap, modified));
    if (!modified.isEmpty()) {
      cache.putAll(modified);
    }
    return new DeployedResourcesSnapshot(
        generator, ownedDeployedResources, deployedResources, deployedResourcesMap);
  }

  private void putOrUpdateLatest(
      HasMetadata generator,
      HasMetadata foundDeployedResource,
      Map<ResourceKey, DeployedResource> deployedResourceMap,
      Map<ResourceKey, DeployedResource> modified) {
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
        DeployedResource updated = DeployedResource.create(
            requiredResource,
            deployedResource.deployed(),
            deployedResource.deployedNode(),
            foundDeployedResource,
            toComparableDeployedNode(requiredResource, foundDeployedResource));
        deployedResourceMap.put(key, updated);
        modified.put(key, updated);
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updated already found resource {} {}.{}",
              foundDeployedResource.getKind(),
              foundDeployedResource.getMetadata().getNamespace(),
              foundDeployedResource.getMetadata().getName());
        }
        DeployedResource updated = DeployedResource.create(
            deployedResource.deployed(),
            deployedResource.deployedNode(),
            foundDeployedResource,
            null);
        deployedResourceMap.put(key, updated);
        modified.put(key, updated);
      }
    } else {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Found resource {} {}.{}",
            foundDeployedResource.getKind(),
            foundDeployedResource.getMetadata().getNamespace(),
            foundDeployedResource.getMetadata().getName());
      }
      DeployedResource created = DeployedResource.create(
          foundDeployedResource,
          null);
      deployedResourceMap.put(key, created);
      modified.put(key, created);
    }
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
