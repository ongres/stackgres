/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;

public interface DeployedResource {

  ResourceKey resourceKey();

  String apiVersion();

  String kind();

  String namespace();

  String name();

  String resourceVersion();

  String[] requiredAnnotations();

  String[] requiredLabels();

  boolean hasRequiredEndpointsWithEmptySubsets();

  boolean isDeployedChanged();

  void traceDeployedChanged(Logger logger);

  boolean isRequiredChanged(ObjectMapper objectMapper, HasMetadata requiredResource);

  boolean hasRequired();

  boolean hasDeployedAnnotations(Map<String, String> annotations);

  boolean hasDeployedLabels(Map<String, String> labels);

  public abstract static class DeployedResourceBuilder {

    protected final ObjectMapper objectMapper;

    public DeployedResourceBuilder(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    public abstract DeployedResource createRequiredDeployed(
        ResourceKey resourceKey,
        HasMetadata generator,
        HasMetadata required,
        HasMetadata deployed);

    public abstract DeployedResource createDeployed(
        HasMetadata deployed);

    public abstract DeployedResource updateRequiredDeployed(
        DeployedResource deployedResource,
        HasMetadata foundDeployed);

    public abstract DeployedResource updateDeployed(
        DeployedResource deployedResource,
        HasMetadata foundDeployed);

    public static String[] requiredAnnotations(HasMetadata required) {
      return Optional.ofNullable(required.getMetadata().getAnnotations())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new);
    }

    public static String[] requiredLabels(HasMetadata required) {
      return Optional.ofNullable(required.getMetadata().getLabels())
          .stream().map(Map::keySet).flatMap(Set::stream).toArray(String[]::new);
    }

    public static boolean hasRequiredEndpointsWithEmptySubsets(HasMetadata required) {
      return Optional.of(required)
          .filter(Endpoints.class::isInstance)
          .map(Endpoints.class::cast)
          .filter(endpoints -> endpoints.getSubsets() == null
              || endpoints.getSubsets().isEmpty())
          .isPresent();
    }

    @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
        justification = "False positive")
    protected ObjectNode toComparableDeployedNode(
        List<String> requiredAnnotations,
        List<String> requiredLabels,
        boolean hasRequiredEndpointsWithEmptySubsets,
        HasMetadata deployed) {
      ObjectNode deployedNode = objectMapper.valueToTree(deployed);
      var deployedMetadata = deployedNode.get("metadata");
      if (deployedMetadata instanceof NullNode) {
        deployedNode.remove("metadata");
      } else if (deployedMetadata != null) {
        ObjectNode comparableDeployedMetadata = objectMapper.createObjectNode();
        JsonNode deployedAnnotations = deployedMetadata.get("annotations");
        if (deployedAnnotations instanceof ObjectNode deployedAnnotationsObject) {
          Seq.seq(deployedAnnotationsObject.fieldNames()).toList().stream()
              .filter(Predicate.not(requiredAnnotations::contains))
              .forEach(deployedAnnotationsObject::remove);
        }
        if (deployedAnnotations == null || deployedAnnotations instanceof NullNode) {
          deployedAnnotations = objectMapper.createObjectNode();
        }
        comparableDeployedMetadata.set("annotations", deployedAnnotations);
        JsonNode deployedLabels = deployedMetadata.get("labels");
        if (deployedLabels instanceof ObjectNode deployedLabelsObject) {
          Seq.seq(deployedLabelsObject.fieldNames()).toList().stream()
              .filter(Predicate.not(requiredLabels::contains))
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
      if (hasRequiredEndpointsWithEmptySubsets) {
        deployedNode.remove("subsets");
      }
      return deployedNode;
    }

  }

}
