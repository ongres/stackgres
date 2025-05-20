/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DeployedResourcesCache {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeployedResourcesCache.class);

  private final Cache<ResourceKey, DeployedResource> cache;
  private final DeployedResource.DeployedResourceBuilder deployedResourceBuilder;
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
    if (propertyContext.getBoolean(
        OperatorProperty.RECONCILIATION_CACHE_ENABLE_HASH)) {
      this.deployedResourceBuilder = new DeployedResourceHashed.DeployedResourceHashedBuilder(objectMapper);
    } else {
      this.deployedResourceBuilder = new DeployedResourceFull.DeployedResourceFullBuilder(objectMapper);
    }
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
        deployedResourceBuilder.createRequiredDeployed(
            key,
            generator,
            requiredResource,
            deployedResource));
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

  public Set<DeployedResource> values() {
    return new HashSet<>(cache.asMap().values());
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
        generator, ownedDeployedResources, deployedResources, deployedResourcesMap, objectMapper);
  }

  private void putOrUpdateLatest(
      HasMetadata generator,
      HasMetadata foundDeployedResource,
      Map<ResourceKey, DeployedResource> deployedResourceMap) {
    ResourceKey key = ResourceKey.create(generator, foundDeployedResource);
    DeployedResource deployedResource = deployedResourceMap.get(key);
    if (deployedResource != null) {
      if (Objects.equals(
          deployedResource.resourceVersion(),
          foundDeployedResource.getMetadata().getResourceVersion())) {
        return;
      }
      if (deployedResource.hasRequired()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updated previously required resource {} {}.{}",
              foundDeployedResource.getKind(),
              foundDeployedResource.getMetadata().getNamespace(),
              foundDeployedResource.getMetadata().getName());
        }
        deployedResourceMap.put(key,
            deployedResourceBuilder.updateRequiredDeployed(
                deployedResource,
                foundDeployedResource));
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updated already found resource {} {}.{}",
              foundDeployedResource.getKind(),
              foundDeployedResource.getMetadata().getNamespace(),
              foundDeployedResource.getMetadata().getName());
        }
        deployedResourceMap.put(key,
            deployedResourceBuilder.updateDeployed(
                deployedResource,
                foundDeployedResource));
      }
    } else {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Found resource {} {}.{}",
            foundDeployedResource.getKind(),
            foundDeployedResource.getMetadata().getNamespace(),
            foundDeployedResource.getMetadata().getName());
      }
      deployedResourceMap.put(key,
          deployedResourceBuilder.createDeployed(
              foundDeployedResource));
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
        .filter(e -> e.getValue().hasDeployedLabels(genericLabels))
        .map(Map.Entry::getKey)
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

}
