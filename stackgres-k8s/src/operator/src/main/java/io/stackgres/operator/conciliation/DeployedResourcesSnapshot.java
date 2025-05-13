/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployedResourcesSnapshot {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeployedResourcesSnapshot.class);

  private final HasMetadata generator;
  private final List<HasMetadata> ownedDeployedResources;
  private final List<HasMetadata> deployedResources;
  private final Map<ResourceKey, DeployedResource> map;
  private final ObjectMapper objectMapper;

  DeployedResourcesSnapshot(
      HasMetadata generator,
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources,
      Map<ResourceKey, DeployedResource> map,
      ObjectMapper objectMapper) {
    this.generator = generator;
    this.ownedDeployedResources = ownedDeployedResources;
    this.deployedResources = deployedResources;
    this.map = map;
    this.objectMapper = objectMapper;
  }

  public List<HasMetadata> ownedDeployedResources() {
    return ownedDeployedResources;
  }

  public List<HasMetadata> deployedResources() {
    return deployedResources;
  }

  public Map<ResourceKey, DeployedResource> map() {
    return map;
  }

  public DeployedResource get(HasMetadata requiredResource) {
    return map.get(ResourceKey.create(generator, requiredResource));
  }

  public Stream<DeployedResource> stream() {
    return map.values().stream();
  }

  public boolean isDeployed(HasMetadata requiredResource) {
    return map.containsKey(ResourceKey.create(generator, requiredResource));
  }

  public boolean isRequiredChanged(HasMetadata requiredResource) {
    boolean result = Optional
        .ofNullable(map.get(ResourceKey.create(generator, requiredResource)))
        .filter(deployedResource -> deployedResource.isRequiredChanged(objectMapper, requiredResource))
        .isPresent();
    if (result && LOGGER.isTraceEnabled()) {
      LOGGER.trace("Detected change for required resource {} {}.{}",
          requiredResource.getKind(),
          requiredResource.getMetadata().getNamespace(),
          requiredResource.getMetadata().getName());
    }
    return result;
  }

  public boolean isDeployedChanged(DeployedResource deployedResourceValue) {
    boolean result = deployedResourceValue.isDeployedChanged();
    if (result && LOGGER.isTraceEnabled()) {
      deployedResourceValue.traceDeployedChanged(LOGGER);
    }
    return result;
  }

  public static DeployedResourcesSnapshot emptySnapshot(HasMetadata generator) {
    return new DeployedResourcesSnapshot(generator, List.of(), List.of(), Map.of(), null);
  }

}
