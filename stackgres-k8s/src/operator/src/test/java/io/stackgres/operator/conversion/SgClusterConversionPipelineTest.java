/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.JsonUtil;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class SgClusterConversionPipelineTest {

  @Inject
  ObjectMapper mapper;

  @Inject
  @Conversion(StackGresCluster.KIND)
  ConversionPipeline pipeline;

  ObjectNode getFromVersion1Resource() {
    return Fixtures.jsonCluster().loadFromVersion1().get();
  }

  ObjectNode getToVersion1beta1Resource() {
    return Fixtures.jsonCluster().loadToVersion1beta1().get();
  }

  ObjectNode getFromVersion1beta1Resource() {
    return Fixtures.jsonCluster().loadFromVersion1beta1().get();
  }

  ObjectNode getToVersion1Resource() {
    return Fixtures.jsonCluster().loadToVersion1().get();
  }

  @Test
  void resourceConversionWithBackupFromVersion1beta1ToVersion1_shouldNotFail() {
    ObjectNode fromVersion1beta1Cluster = getFromVersion1beta1Resource();
    ObjectNode toVersion1Cluster = getToVersion1Resource();
    JsonUtil.assertJsonEquals(toVersion1Cluster,
        pipeline.convert(
            ConversionUtil.API_VERSION_1,
            ImmutableList.of(fromVersion1beta1Cluster)).get(0));
  }

  @Test
  void resourceConversionWithBackupFromVersion1ToVersion1beta1_shouldNotFail() {
    ObjectNode fromVersion1Cluster = getFromVersion1Resource();
    ObjectNode toVersion1beta1Cluster = getToVersion1beta1Resource();
    JsonUtil.assertJsonEquals(toVersion1beta1Cluster,
        pipeline.convert(
            ConversionUtil.API_VERSION_1BETA1,
            ImmutableList.of(fromVersion1Cluster)).get(0));
  }

  @ParameterizedTest
  @CsvSource({"latest,12.6", "11,11.11", "12,12.6", "12.4,12.4"})
  void resourceConversionVersion1beta1ToVersion1_upgradePostgresVersion_shouldNotFail(String from,
      String to) {
    ObjectNode fromVersion1beta1Cluster =
        changePostgresVersion(getFromVersion1beta1Resource(), from);
    ObjectNode toVersion1Cluster =
        changePostgresVersion(getToVersion1Resource(), to);
    JsonUtil.assertJsonEquals(toVersion1Cluster,
        pipeline.convert(
            ConversionUtil.API_VERSION_1,
            ImmutableList.of(fromVersion1beta1Cluster)).get(0));
  }

  private ObjectNode changePostgresVersion(ObjectNode node, String postgresVersion) {
    Optional.ofNullable(node.get("spec"))
        .map(ObjectNode.class::cast)
        .ifPresent(spec -> {
          if (spec.get("postgresVersion") != null) {
            spec.set("postgresVersion", mapper.convertValue(
                postgresVersion, JsonNode.class));
          } else if (spec.get("postgres") != null) {
            ObjectNode jsonNode = (ObjectNode) spec.get("postgres");
            jsonNode.set("version", mapper.convertValue(
                postgresVersion, JsonNode.class));
          }
        });
    return node;
  }

}
