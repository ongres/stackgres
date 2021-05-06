/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.nio.file.Paths;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresProperty;
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
    if (StackGresProperty.OPERATOR_VERSION.getString().endsWith("-SNAPSHOT")) {
      imageTag = "^development(-[^-]+)?-jvm$";
    } else {
      imageTag = "^" + Pattern.quote(StackGresProperty.OPERATOR_VERSION.getString()) + "$";
    }
    Assert.assertTrue(operatorConfig.get("operator").get("image").get("tag").asText()
        + " should match " + imageTag,
        operatorConfig.get("operator").get("image").get("tag").asText()
            .matches(imageTag));
    Assert.assertEquals(OperatorProperty.PROMETHEUS_AUTOBIND.getString(),
        operatorConfig.get("prometheus").get("allowAutobind").asText());
  }

}
