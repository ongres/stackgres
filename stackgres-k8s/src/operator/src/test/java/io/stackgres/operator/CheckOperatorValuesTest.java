/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.stackgres.operator.app.StackGresOperatorApp;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresUtil;

import org.jooq.lambda.Seq;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(CheckOperatorValuesExtension.class)
public class CheckOperatorValuesTest {

  @Test
  public void checkOperatorValues() throws Exception {
    ObjectMapper objectMapper = new YAMLMapper();
    JsonNode operatorConfig = objectMapper.readTree(
        Paths.get("../../install/helm/stackgres-operator/values.yaml").toFile());
    final String imageTag;
    if (StackGresUtil.OPERATOR_VERSION.endsWith("-SNAPSHOT")) {
      imageTag = "development-jvm";
    } else {
      imageTag = StackGresUtil.OPERATOR_VERSION + "-jvm";
    }
    Assert.assertEquals(imageTag,
        operatorConfig.get("image").get("tag").asText());
    Assert.assertEquals(StackGresUtil.PROMETHEUS_AUTOBIND,
        operatorConfig.get("prometheus").get("allowAutobind").asText());
  }

  @Test
  public void checkComponentVersions() throws Exception {
    ObjectMapper objectMapper = new YAMLMapper();
    JsonNode versions = objectMapper.readTree(
        new URL("https://stackgres.io/downloads/stackgres-k8s/stackgres/components/"
            + StackGresUtil.CONTAINER_BUILD + "/versions.yaml"));
    Properties properties = new Properties();
    properties.load(StackGresOperatorApp.class.getResourceAsStream("/versions.properties"));
    Assert.assertArrayEquals(
        Seq.seq((ArrayNode) versions.get("components").get("postgresql").get("versions"))
        .map(JsonNode::asText)
        .toArray(),
        StackGresComponents.getAsArray("postgresql"));
    Assert.assertEquals(
        versions.get("components").get("patroni").get("versions").get(0).asText(),
        StackGresComponents.get("patroni"));
    Assert.assertEquals(
        versions.get("components").get("wal_g").get("versions").asText(),
        StackGresComponents.get("wal_g"));
    Assert.assertEquals(
        versions.get("components").get("pgbouncer").get("versions").get(0).asText(),
        StackGresComponents.get("pgbouncer"));
    Assert.assertEquals(
        versions.get("components").get("postgres_exporter").get("versions").get(0).asText(),
        StackGresComponents.get("postgres_exporter"));
  }

}
