/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;

public record DeployedResourceFull(
    ResourceKey resourceKey,
    Optional<HasMetadata> required,
    HasMetadata deployed,
    ObjectNode deployedNode,
    HasMetadata foundDeployed,
    ObjectNode foundDeployedNode) implements DeployedResource {

  @Override
  public String apiVersion() {
    return foundDeployed.getApiVersion();
  }

  @Override
  public String kind() {
    return foundDeployed.getKind();
  }

  @Override
  public String namespace() {
    return foundDeployed.getMetadata().getNamespace();
  }

  @Override
  public String name() {
    return foundDeployed.getMetadata().getName();
  }

  @Override
  public String resourceVersion() {
    return foundDeployed.getMetadata().getResourceVersion();
  }

  @Override
  public String[] requiredAnnotations() {
    return required.map(DeployedResourceBuilder::requiredAnnotations).orElse(new String[0]);
  }

  @Override
  public String[] requiredLabels() {
    return required.map(DeployedResourceBuilder::requiredLabels).orElse(new String[0]);
  }

  @Override
  public boolean hasRequiredEndpointsWithEmptySubsets() {
    return required.map(DeployedResourceBuilder::hasRequiredEndpointsWithEmptySubsets).orElse(false);
  }

  @Override
  public boolean isDeployedChanged() {
    return deployedNode() == null
        || foundDeployedNode() == null
        || !deployedNode().equals(foundDeployedNode());
  }

  @Override
  public void traceDeployedChanged(Logger logger) {
    HasMetadata foundDeployed = foundDeployed();
    logger.trace("Detected change for deployed resource {} {}.{}",
        foundDeployed.getKind(),
        foundDeployed.getMetadata().getNamespace(),
        foundDeployed.getMetadata().getName());
    if (deployedNode() != null
        && foundDeployedNode() != null) {
      try {
        JsonNode diffs = JsonDiff.asJson(
            deployedNode(),
            foundDeployedNode());
        logger.trace("Diff {}", diffs);
      } catch (Exception ex) {
        logger.warn("Diff failed for {} and {}",
            deployedNode(),
            foundDeployedNode(),
            ex);
      }
    }
  }

  @Override
  public boolean isRequiredChanged(ObjectMapper objectMapper, HasMetadata requiredResource) {
    return required
        .filter(Predicate.not(requiredResource::equals))
        .isPresent();
  }

  @Override
  public boolean hasRequired() {
    return required.isPresent();
  }

  @Override
  public boolean hasDeployedLabels(Map<String, String> labels) {
    Map<String, String> deployedLabels = Optional
        .ofNullable(foundDeployed().getMetadata().getLabels())
        .orElse(Map.of());
    return labels.entrySet()
        .stream()
        .allMatch(label -> deployedLabels.entrySet().stream().anyMatch(label::equals));
  }

  public static class DeployedResourceFullBuilder extends DeployedResource.DeployedResourceBuilder {

    public DeployedResourceFullBuilder(ObjectMapper objectMapper) {
      super(objectMapper);
    }

    @Override
    public DeployedResourceFull createRequiredDeployed(
        ResourceKey resourceKey,
        HasMetadata generator,
        HasMetadata required,
        HasMetadata deployed) {
      final ObjectNode deployedNode = toComparableDeployedNode(
          Arrays.asList(DeployedResourceBuilder.requiredAnnotations(required)),
          Arrays.asList(DeployedResourceBuilder.requiredLabels(required)),
          DeployedResourceBuilder.hasRequiredEndpointsWithEmptySubsets(required),
          deployed);
      return new DeployedResourceFull(
          resourceKey,
          Optional.of(required),
          deployed,
          deployedNode,
          deployed,
          deployedNode);
    }

    @Override
    public DeployedResourceFull createDeployed(
        HasMetadata deployed) {
      return new DeployedResourceFull(
          null,
          Optional.empty(),
          deployed,
          null,
          deployed,
          null);
    }

    @Override
    public DeployedResourceFull updateRequiredDeployed(
        DeployedResource deployedResource,
        HasMetadata foundDeployed) {
      HasMetadata required = DeployedResourceFull.class.cast(deployedResource).required().get();
      final ObjectNode foundDeployedNode = toComparableDeployedNode(
          Arrays.asList(DeployedResourceBuilder.requiredAnnotations(required)),
          Arrays.asList(DeployedResourceBuilder.requiredLabels(required)),
          DeployedResourceBuilder.hasRequiredEndpointsWithEmptySubsets(required),
          foundDeployed);
      return new DeployedResourceFull(
          DeployedResourceFull.class.cast(deployedResource).resourceKey(),
          Optional.of(required),
          DeployedResourceFull.class.cast(deployedResource).deployed(),
          DeployedResourceFull.class.cast(deployedResource).deployedNode(),
          foundDeployed,
          foundDeployedNode);
    }

    @Override
    public DeployedResourceFull updateDeployed(
        DeployedResource deployedResource,
        HasMetadata foundDeployed) {
      return new DeployedResourceFull(
          null,
          Optional.empty(),
          DeployedResourceFull.class.cast(deployedResource).deployed(),
          DeployedResourceFull.class.cast(deployedResource).deployedNode(),
          foundDeployed,
          null);
    }

  }

}
