/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployedResourcesCacheSnapshot implements DeployedResourcesSnapshot {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeployedResourcesCacheSnapshot.class);

  private final HasMetadata generator;
  private final List<HasMetadata> ownedDeployedResources;
  private final List<HasMetadata> deployedResources;
  private final Map<ResourceKey, DeployedResource> map;

  DeployedResourcesCacheSnapshot(
      HasMetadata generator,
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources,
      Map<ResourceKey, DeployedResource> map) {
    this.generator = generator;
    this.ownedDeployedResources = ownedDeployedResources;
    this.deployedResources = deployedResources;
    this.map = map;
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
  public boolean isChanged(HasMetadata requiredResource, DeployedResource deployedResourceValue) {
    return isRequiredChanged(requiredResource) || isDeployedChanged(deployedResourceValue);
  }

  private boolean isRequiredChanged(HasMetadata requiredResource) {
    boolean result = Optional
        .ofNullable(map.get(ResourceKey.create(generator, requiredResource)))
        .map(DeployedResource::required)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(Predicate.not(requiredResource::equals))
        .isPresent();
    if (result && LOGGER.isTraceEnabled()) {
      LOGGER.trace("Detected change for required resource {} {}.{}",
          requiredResource.getKind(),
          requiredResource.getMetadata().getNamespace(),
          requiredResource.getMetadata().getName());
    }
    return result;
  }

  private boolean isDeployedChanged(DeployedResource deployedResourceValue) {
    boolean result = deployedResourceValue.deployedNode() == null
        || deployedResourceValue.foundDeployedNode() == null
        || !deployedResourceValue.deployedNode()
          .equals(deployedResourceValue.foundDeployedNode());
    if (result && LOGGER.isTraceEnabled()) {
      HasMetadata foundDeployed = deployedResourceValue.foundDeployed();
      LOGGER.trace("Detected change for deployed resource {} {}.{}",
          foundDeployed.getKind(),
          foundDeployed.getMetadata().getNamespace(),
          foundDeployed.getMetadata().getName());
      if (deployedResourceValue.deployedNode() != null
          && deployedResourceValue.foundDeployedNode() != null) {
        try {
          JsonNode diffs = JsonDiff.asJson(
              deployedResourceValue.deployedNode(),
              deployedResourceValue.foundDeployedNode());
          LOGGER.trace("Diff {}", diffs);
        } catch (Exception ex) {
          LOGGER.warn("Diff failed for {} and {}",
              deployedResourceValue.deployedNode(),
              deployedResourceValue.foundDeployedNode(),
              ex);
        }
      }
    }
    return result;
  }

}
