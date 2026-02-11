/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.io.InputStream;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.JsonMapperCustomizer;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.KubernetesSchemaTestUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CrdSchemaCompatibilityTest {

  @ParameterizedTest
  @ValueSource(classes = {
      StackGresConfig.class,
      StackGresCluster.class,
      StackGresProfile.class,
      StackGresPostgresConfig.class,
      StackGresPoolingConfig.class,
      StackGresBackup.class,
      StackGresDistributedLogs.class,
      StackGresDbOps.class,
      StackGresObjectStorage.class,
      StackGresScript.class,
      StackGresShardedCluster.class,
      StackGresShardedBackup.class,
      StackGresShardedDbOps.class,
      StackGresStream.class,
  })
  void assertSchemaCompatibility(Class<?> crdClass) throws Exception {
    // 1. Get KIND constant from CRD class
    String kind = (String) crdClass.getField("KIND").get(null);

    // 2. Load CRD YAML directly as JsonNode
    JsonNode schema;
    try (InputStream is = crdClass.getResourceAsStream("/crds/" + kind + ".yaml")) {
      JsonNode crdTree = JsonUtil.yamlMapper().readTree(is);
      schema = crdTree.at("/spec/versions/0/schema/openAPIV3Schema");
    }

    // 3. Generate random spec and status from schema
    JsonNode specSchema = schema.at("/properties/spec");
    JsonNode statusSchema = schema.at("/properties/status");

    ObjectNode expected = JsonUtil.jsonMapper().createObjectNode();
    if (!specSchema.isMissingNode()) {
      expected.set("spec", KubernetesSchemaTestUtil.createWithRandomData(specSchema));
    }
    if (!statusSchema.isMissingNode()) {
      expected.set("status", KubernetesSchemaTestUtil.createWithRandomData(statusSchema));
    }

    // 4. Build full JSON and deserialize to POJO
    ObjectMapper objectMapper = new ObjectMapper();
    new JsonMapperCustomizer().customize(objectMapper);

    ObjectNode fullJson = expected.deepCopy();
    fullJson.put("apiVersion", "stackgres.io/v1");
    fullJson.put("kind", kind);
    fullJson.putObject("metadata").put("name", "test").put("namespace", "test");

    Object pojo = objectMapper.readValue(fullJson.toString(), crdClass);

    // 5. Serialize POJO back to JsonNode
    JsonNode reserialized = objectMapper.valueToTree(pojo);

    // 6. Extract only spec/status from reserialized output
    ObjectNode actual = JsonUtil.jsonMapper().createObjectNode();
    if (reserialized.has("spec")) {
      actual.set("spec", reserialized.get("spec"));
    }
    if (reserialized.has("status")) {
      actual.set("status", reserialized.get("status"));
    }

    // Handle Void-status CRDs (e.g., StackGresProfile, StackGresObjectStorage)
    if (!actual.has("status")) {
      expected.remove("status");
    }

    // 7. Strip nulls from both trees
    stripNulls(expected);
    stripNulls(actual);

    overrides(crdClass, actual, expected);

    // 8. Compare
    JsonUtil.assertJsonEquals(expected, actual);
  }

  private void overrides(Class<?> crdClass, ObjectNode actual, ObjectNode expected) {
    if (crdClass == StackGresConfig.class) {
      overrideForStackGresConfig(actual, expected);
      return;
    }
    if (crdClass == StackGresCluster.class) {
      overrideForStackGresCluster(actual, expected);
      return;
    }
    if (crdClass == StackGresDistributedLogs.class) {
      overrideForStackGresDistributedLogs(actual, expected);
      return;
    }
    if (crdClass == StackGresShardedCluster.class) {
      overrideForStackGresShardedCluster(actual, expected);
      return;
    }
  }

  private void overrideForStackGresConfig(ObjectNode actual, ObjectNode expected) {
    Seq
        .seq(expected
            .get("spec")
            .get("collector")
            .get("prometheusOperator")
            .get("monitors")
            .elements())
        .map(ObjectNode.class::cast)
        .zipWithIndex()
        .forEach(monitor -> {
          final ObjectNode actualMonitor = (ObjectNode) actual
              .get("spec")
              .get("collector")
              .get("prometheusOperator")
              .get("monitors")
              .get(monitor.v2.intValue());
          monitor.v1.set("spec", NullNode.instance);
          actualMonitor.set("spec", NullNode.instance);
          Seq
              .seq(monitor.v1
                  .get("metadata")
                  .get("ownerReferences")
                  .elements())
              .map(ObjectNode.class::cast)
              .zipWithIndex()
              .forEach(ownerReference -> {
                final JsonNode actualOwnerReference = actualMonitor
                    .get("metadata")
                    .get("ownerReferences")
                    .get(ownerReference.v2.intValue());
                ownerReference.v1.set("apiVersion", actualOwnerReference.get("apiVersion"));
                ownerReference.v1.set("kind", actualOwnerReference.get("kind"));
              });
        });
  }

  private void overrideForStackGresCluster(ObjectNode actual, ObjectNode expected) {
    removeServiceIgnoredProperties((ObjectNode) expected
        .get("spec")
        .get("postgresServices")
        .get("primary"));
    removeServiceIgnoredProperties((ObjectNode) expected
        .get("spec")
        .get("postgresServices")
        .get("replicas"));
  }

  private void overrideForStackGresDistributedLogs(ObjectNode actual, ObjectNode expected) {
    removeServiceIgnoredProperties((ObjectNode) expected
        .get("spec")
        .get("postgresServices")
        .get("primary"));
    removeServiceIgnoredProperties((ObjectNode) expected
        .get("spec")
        .get("postgresServices")
        .get("replicas"));
  }

  private void overrideForStackGresShardedCluster(ObjectNode actual, ObjectNode expected) {
    removeServiceIgnoredProperties((ObjectNode) expected
        .get("spec")
        .get("postgresServices")
        .get("coordinator")
        .get("primary"));
    removeServiceIgnoredProperties((ObjectNode) expected
        .get("spec")
        .get("postgresServices")
        .get("coordinator")
        .get("any"));
    removeServiceIgnoredProperties((ObjectNode) expected
        .get("spec")
        .get("postgresServices")
        .get("shards")
        .get("primaries"));
  }

  public void removeServiceIgnoredProperties(ObjectNode service) {
    for (String ignoreProperty : new String[]
        {
        "clusterIP", "clusterIPs", "externalName",
        "ports", "publishNotReadyAddresses", "selector"
        }) {
      service.remove(ignoreProperty);
    }
  }

  static void stripNulls(JsonNode node) {
    Iterator<JsonNode> it = node.iterator();
    while (it.hasNext()) {
      JsonNode child = it.next();
      if (child.isNull()) {
        it.remove();
      } else {
        stripNulls(child);
      }
    }
  }

}
