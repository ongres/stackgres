/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.stackgres.operator.common.StackGresUtil;

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
    Assert.assertEquals(StackGresUtil.CRD_GROUP,
        operatorConfig.get("group").asText());
    Assert.assertEquals(StackGresUtil.CRD_VERSION,
        operatorConfig.get("crd").get("version").asText());
    Assert.assertEquals(StackGresUtil.CONTAINER_BUILD,
        operatorConfig.get("containerBuild").asText());
    Assert.assertEquals(StackGresUtil.PROMETHEUS_AUTOBIND,
        operatorConfig.get("prometheus").get("allowAutobind").asText());
  }

}
