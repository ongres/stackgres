/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgpgconfig;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.operator.conversion.ConversionUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConvertDefaultParametersPostVersion1Test {

  protected static final JsonMapper MAPPER = new JsonMapper();

  protected ConvertDefaultParametersPostVersion1 converter;
  protected ObjectNode version1PgConfig;
  protected ObjectNode version1beta1PgConfig;

  ObjectNode getVersion1Resource() {
    ObjectNode resource = JsonUtil.readFromJsonAsJson("postgres_config/version1.json");
    ObjectNode status = MAPPER.createObjectNode();
    ObjectNode defaultParameters = MAPPER.createObjectNode();
    PostgresDefaultValues.getDefaultValues()
        .forEach(defaultParameters::put);
    status.set("defaultParameters", defaultParameters);
    resource.set("status", status);
    return resource;
  }

  ObjectNode getVersion1beta1Resource() {
    ObjectNode resource = JsonUtil.readFromJsonAsJson("postgres_config/version1beta1.json");
    ObjectNode parameters = (ObjectNode) resource.get("spec").get("postgresql.conf");
    ObjectNode status = MAPPER.createObjectNode();
    ArrayNode defaultParameters = MAPPER.createArrayNode();
    PostgresDefaultValues.getDefaultValues()
        .entrySet()
        .stream()
        .filter(parameter -> !parameters.has(parameter.getKey())
            || parameters.get(parameter.getKey()).asText()
                .equals(parameter.getValue()))
        .forEach(parameter -> defaultParameters.add(parameter.getKey()));
    status.set("defaultParameters", defaultParameters);
    resource.set("status", status);
    return resource;
  }

  @BeforeEach
  void setUp() {
    converter = new ConvertDefaultParametersPostVersion1();
    version1PgConfig = getVersion1Resource();
    version1beta1PgConfig = getVersion1beta1Resource();
  }

  @Test
  void resourceConversionWithBackupFromVersion1beta1ToVersion1_shouldNotFail() {
    version1PgConfig.put("apiVersion", ConversionUtil.API_VERSION_1BETA1);
    JsonUtil.assertJsonEquals(version1PgConfig,
        converter.convert(
            ConversionUtil.apiVersionAsNumberOf(version1beta1PgConfig),
            ConversionUtil.VERSION_1,
            version1beta1PgConfig));
  }

  @Test
  void resourceConversionWithBackupFromVersion1ToVersion1beta1_shouldNotFail() {
    version1beta1PgConfig.put("apiVersion", ConversionUtil.API_VERSION_1);
    JsonUtil.assertJsonEquals(version1beta1PgConfig,
        converter.convert(
            ConversionUtil.apiVersionAsNumberOf(version1PgConfig),
            ConversionUtil.VERSION_1BETA1,
            version1PgConfig));
  }

}
