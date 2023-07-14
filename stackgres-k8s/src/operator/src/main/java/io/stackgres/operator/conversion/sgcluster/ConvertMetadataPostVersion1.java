/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgcluster;

import static io.stackgres.operator.conversion.ConversionUtil.VERSION_1;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conversion.Conversion;
import io.stackgres.operator.conversion.Converter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Conversion(StackGresCluster.KIND)
public class ConvertMetadataPostVersion1 implements Converter {

  @Override
  public ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node) {
    if (desiredVersion >= VERSION_1 && originalVersion < VERSION_1) {
      Optional.ofNullable(node.get("spec").get("pods"))
          .map(pods -> pods.get("metadata"))
          .ifPresent(podmeta -> {
            JsonNode labels = podmeta.get("labels");
            if (labels != null) {
              if (node.get("spec").has("metadata")) {
                ObjectNode metadata = (ObjectNode) node.get("spec").get("metadata");
                metadata.putObject("labels").set("clusterPods", labels);
              } else {
                ObjectNode metadata = ((ObjectNode) node.get("spec")).putObject("metadata");
                metadata.putObject("labels").set("clusterPods", labels);
              }
            }
            ((ObjectNode) node.get("spec").get("pods")).remove("metadata");
          });
      Optional.ofNullable(node.get("spec").get("metadata"))
          .map(pods -> pods.get("annotations"))
          .map(ObjectNode.class::cast)
          .ifPresent(annotations -> {
            JsonNode podsAnnotations = annotations.get("pods");
            if (podsAnnotations != null) {
              annotations.set("clusterPods", podsAnnotations);
              annotations.remove("pods");
            }
          });

      Optional.ofNullable(node.get("spec").get("postgresServices"))
          .map(pods -> pods.get("primary"))
          .ifPresent(primary -> {
            JsonNode annotations = primary.get("annotations");
            if (annotations != null) {
              if (node.get("spec").has("metadata")) {
                ObjectNode metadata = (ObjectNode) node.get("spec").get("metadata");
                if (!metadata.has("annotations")) {
                  metadata.putObject("annotations");
                }
                ((ObjectNode) metadata.get("annotations")).set("primaryService", annotations);
              } else {
                ObjectNode metadata = ((ObjectNode) node.get("spec")).putObject("metadata");
                metadata.putObject("annotations").set("primaryService", annotations);
              }
            }
            ((ObjectNode) node.get("spec").get("postgresServices").get("primary"))
                .remove("annotations");
          });
      Optional.ofNullable(node.get("spec").get("postgresServices"))
          .map(pods -> pods.get("replicas"))
          .ifPresent(primary -> {
            JsonNode annotations = primary.get("annotations");
            if (annotations != null) {
              if (node.get("spec").has("metadata")) {
                ObjectNode metadata = (ObjectNode) node.get("spec").get("metadata");
                if (!metadata.has("annotations")) {
                  metadata.putObject("annotations");
                }
                ((ObjectNode) metadata.get("annotations")).set("replicasService", annotations);
              } else {
                ObjectNode metadata = ((ObjectNode) node.get("spec")).putObject("metadata");
                metadata.putObject("annotations").set("replicasService", annotations);
              }
            }
            ((ObjectNode) node.get("spec").get("postgresServices").get("replicas"))
                .remove("annotations");
          });
    } else if (desiredVersion < VERSION_1 && originalVersion >= VERSION_1) {
      Optional.ofNullable(node.get("spec").get("metadata"))
          .map(metadata -> metadata.get("labels"))
          .ifPresent(labels -> {
            JsonNode clusterPodsLabels = labels.get("clusterPods");
            if (clusterPodsLabels != null) {
              if (node.get("spec").has("pods")) {
                ObjectNode pods = (ObjectNode) node.get("spec").get("pods");
                pods.putObject("metadata").set("labels", clusterPodsLabels);
              } else {
                ObjectNode pods = ((ObjectNode) node.get("spec")).putObject("pods");
                pods.putObject("metadata").set("labels", clusterPodsLabels);
              }
            }
            ((ObjectNode) node.get("spec").get("metadata")).remove("labels");
          });
      Optional.ofNullable(node.get("spec").get("metadata"))
          .map(pods -> pods.get("annotations"))
          .map(ObjectNode.class::cast)
          .ifPresent(annotations -> {
            JsonNode podsAnnotations = annotations.get("clusterPods");
            if (podsAnnotations != null) {
              annotations.set("pods", podsAnnotations);
              annotations.remove("clusterPods");
            }
          });

      Optional.ofNullable(node.get("spec").get("metadata"))
          .map(pods -> pods.get("annotations"))
          .ifPresent(primary -> {
            JsonNode annotations = primary.get("primaryService");
            if (annotations != null) {
              ObjectNode metadata = (ObjectNode) node.get("spec").get("postgresServices")
                  .get("primary");
              metadata.set("annotations", annotations);
            }
            ((ObjectNode) node.get("spec").get("metadata").get("annotations"))
                .remove("primaryService");
          });
      Optional.ofNullable(node.get("spec").get("metadata"))
          .map(pods -> pods.get("annotations"))
          .ifPresent(replicas -> {
            JsonNode annotations = replicas.get("replicasService");
            if (annotations != null) {
              ObjectNode metadata = (ObjectNode) node.get("spec").get("postgresServices")
                  .get("replicas");
              metadata.set("annotations", annotations);
            }
            ((ObjectNode) node.get("spec").get("metadata").get("annotations"))
                .remove("replicasService");
          });
    }
    return node;
  }

}
