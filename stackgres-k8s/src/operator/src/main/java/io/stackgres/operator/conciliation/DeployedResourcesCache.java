/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface DeployedResourcesCache {

<<<<<<< Updated upstream
  void put(
=======
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
        .or(() -> Optional.of(propertyContext.get(OperatorProperty.RECONCILIATION_PERIOD)
            .map(Integer::valueOf)
            .orElse(60) * 10))
        .ifPresent(duration -> cacheBuilder.expireAfterWrite(Duration.ofSeconds(duration)));
    propertyContext.get(
        OperatorProperty.RECONCILIATION_CACHE_SIZE)
        .map(Integer::valueOf)
        .ifPresent(size -> cacheBuilder.maximumSize(size));
    this.cache = cacheBuilder.build();
    this.objectMapper = objectMapper;
  }

  public void put(
>>>>>>> Stashed changes
      HasMetadata generator,
      HasMetadata requiredResource,
      HasMetadata deployedResource);

  void remove(
      HasMetadata generator,
      HasMetadata deletedResource);

  void removeAll(
      HasMetadata generator);

  DeployedResource get(
      HasMetadata generator,
      HasMetadata requiredResource);

  DeployedResourcesSnapshot createDeployedResourcesSnapshot(
      HasMetadata generator,
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources);

  void removeWithLabelsNotIn(
      HasMetadata generator,
      Map<String, String> genericLabels,
      List<HasMetadata> deployedResources);

}
