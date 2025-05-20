/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresUtil;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;

public record DeployedResourceHashed(
    ResourceKey resourceKey,
    Optional<String> required,
    String deployed,
    String deployedNode,
    String foundDeployed,
    String foundDeployedNode,
    String apiVersion,
    String kind,
    String namespace,
    String name,
    String resourceVersion,
    String[] requiredAnnotations,
    String[] requiredLabels,
    boolean hasRequiredEndpointsWithEmptySubsets,
    String[][] deployedAnnotations,
    String[][] deployedLabels) implements DeployedResource {

  @Override
  public boolean isDeployedChanged() {
    return deployedNode() == null
        || foundDeployedNode() == null
        || !deployedNode().equals(foundDeployedNode());
  }

  @Override
  public void traceDeployedChanged(Logger logger) {
    logger.trace("Detected change for deployed resource {} {}.{}",
        kind(),
        namespace(),
        name());
  }

  @Override
  public boolean isRequiredChanged(ObjectMapper objectMapper, HasMetadata requiredResource) {
    return required
        .filter(Predicate.not(hashOf(objectMapper, requiredResource)::equals))
        .isPresent();
  }

  @Override
  public boolean hasRequired() {
    return required.isPresent();
  }

  @Override
  public boolean hasDeployedAnnotations(Map<String, String> annotations) {
    return annotations.entrySet()
        .stream()
        .allMatch(annotation -> Arrays.asList(deployedAnnotations).stream()
            .map(deployedAnnotation -> Map.entry(deployedAnnotation[0], deployedAnnotation[1]))
            .anyMatch(annotation::equals));
  }

  @Override
  public boolean hasDeployedLabels(Map<String, String> labels) {
    return labels.entrySet()
        .stream()
        .allMatch(label -> Arrays.asList(deployedLabels).stream()
            .map(deployedLabel -> Map.entry(deployedLabel[0], deployedLabel[1]))
            .anyMatch(label::equals));
  }

  public static class DeployedResourceHashedBuilder extends DeployedResource.DeployedResourceBuilder {

    public DeployedResourceHashedBuilder(ObjectMapper objectMapper) {
      super(objectMapper);
    }

    @Override
    public DeployedResourceHashed createRequiredDeployed(
        ResourceKey resourceKey,
        HasMetadata generator,
        HasMetadata required,
        HasMetadata deployed) {
      final ObjectNode deployedNode = toComparableDeployedNode(
          Arrays.asList(DeployedResourceBuilder.requiredAnnotations(required)),
          Arrays.asList(DeployedResourceBuilder.requiredLabels(required)),
          DeployedResourceBuilder.hasRequiredEndpointsWithEmptySubsets(required),
          deployed);
      return new DeployedResourceHashed(
          resourceKey,
          Optional.of(hashOf(required)),
          hashOf(deployed),
          hashOf(deployedNode),
          hashOf(deployed),
          hashOf(deployedNode),
          deployed.getApiVersion(),
          deployed.getKind(),
          deployed.getMetadata().getNamespace(),
          deployed.getMetadata().getName(),
          deployed.getMetadata().getResourceVersion(),
          Optional.ofNullable(deployed.getMetadata().getAnnotations())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          Optional.ofNullable(deployed.getMetadata().getLabels())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          Optional.of(required)
          .filter(Endpoints.class::isInstance)
          .map(Endpoints.class::cast)
          .filter(endpoints -> endpoints.getSubsets() == null
              || endpoints.getSubsets().isEmpty())
          .isPresent(),
          Optional.ofNullable(deployed.getMetadata().getAnnotations())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new),
          Optional.ofNullable(deployed.getMetadata().getLabels())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new));
    }

    @Override
    public DeployedResourceHashed createDeployed(
        HasMetadata deployed) {
      return new DeployedResourceHashed(
          null,
          Optional.empty(),
          hashOf(deployed),
          null,
          hashOf(deployed),
          null,
          deployed.getApiVersion(),
          deployed.getKind(),
          deployed.getMetadata().getNamespace(),
          deployed.getMetadata().getName(),
          deployed.getMetadata().getResourceVersion(),
          Optional.ofNullable(deployed.getMetadata().getAnnotations())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          Optional.ofNullable(deployed.getMetadata().getLabels())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          false,
          Optional.ofNullable(deployed.getMetadata().getAnnotations())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new),
          Optional.ofNullable(deployed.getMetadata().getLabels())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new));
    }

    @Override
    public DeployedResourceHashed updateRequiredDeployed(
        DeployedResource deployedResource,
        HasMetadata foundDeployed) {
      String required = DeployedResourceHashed.class.cast(deployedResource).required().get();
      final ObjectNode foundDeployedNode = toComparableDeployedNode(
          Arrays.asList(deployedResource.requiredAnnotations()),
          Arrays.asList(deployedResource.requiredLabels()),
          deployedResource.hasRequiredEndpointsWithEmptySubsets(),
          foundDeployed);
      return new DeployedResourceHashed(
          DeployedResourceHashed.class.cast(deployedResource).resourceKey(),
          Optional.of(required),
          DeployedResourceHashed.class.cast(deployedResource).deployed(),
          DeployedResourceHashed.class.cast(deployedResource).deployedNode(),
          hashOf(foundDeployed),
          hashOf(foundDeployedNode),
          foundDeployed.getApiVersion(),
          foundDeployed.getKind(),
          foundDeployed.getMetadata().getNamespace(),
          foundDeployed.getMetadata().getName(),
          foundDeployed.getMetadata().getResourceVersion(),
          Optional.ofNullable(foundDeployed.getMetadata().getAnnotations())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          Optional.ofNullable(foundDeployed.getMetadata().getLabels())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          Optional.of(required)
          .filter(Endpoints.class::isInstance)
          .map(Endpoints.class::cast)
          .filter(endpoints -> endpoints.getSubsets() == null
              || endpoints.getSubsets().isEmpty())
          .isPresent(),
          Optional.ofNullable(foundDeployed.getMetadata().getAnnotations())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new),
          Optional.ofNullable(foundDeployed.getMetadata().getLabels())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new));
    }

    @Override
    public DeployedResourceHashed updateDeployed(
        DeployedResource deployedResource,
        HasMetadata foundDeployed) {
      return new DeployedResourceHashed(
          null,
          Optional.empty(),
          DeployedResourceHashed.class.cast(deployedResource).deployed(),
          DeployedResourceHashed.class.cast(deployedResource).deployedNode(),
          hashOf(foundDeployed),
          null,
          foundDeployed.getApiVersion(),
          foundDeployed.getKind(),
          foundDeployed.getMetadata().getNamespace(),
          foundDeployed.getMetadata().getName(),
          foundDeployed.getMetadata().getResourceVersion(),
          Optional.ofNullable(foundDeployed.getMetadata().getAnnotations())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          Optional.ofNullable(foundDeployed.getMetadata().getLabels())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new),
          false,
          Optional.ofNullable(foundDeployed.getMetadata().getAnnotations())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new),
          Optional.ofNullable(foundDeployed.getMetadata().getLabels())
          .stream().map(Map::entrySet).flatMap(Set::stream)
          .map(entry -> new String[] { entry.getKey(), entry.getValue() })
          .toArray(String[][]::new));
    }

    private String hashOf(HasMetadata resource) {
      return DeployedResourceHashed.hashOf(objectMapper, resource);
    }

    private String hashOf(ObjectNode resourceNode) {
      return DeployedResourceHashed.hashOf(objectMapper, resourceNode);
    }
  }

  private static String hashOf(ObjectMapper objectMapper, HasMetadata resource) {
    return StackGresUtil.getMd5Sum(
        Unchecked.supplier(() -> objectMapper.writeValueAsString(resource)).get());
  }

  private static String hashOf(ObjectMapper objectMapper, ObjectNode resourceNode) {
    return StackGresUtil.getMd5Sum(
        Unchecked.supplier(() -> objectMapper.writeValueAsString(resourceNode)).get());
  }

}
