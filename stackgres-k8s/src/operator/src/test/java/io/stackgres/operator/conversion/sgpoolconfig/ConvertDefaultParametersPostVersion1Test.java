/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgpoolconfig;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
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
    ObjectNode resource = JsonUtil.readFromJsonAsJson("pooling_config/version1.json");
    ObjectNode status = MAPPER.createObjectNode();
    ObjectNode pgBouncer = MAPPER.createObjectNode();
    ObjectNode defaultParameters = MAPPER.createObjectNode();
    PgBouncerDefaultValues.getDefaultValues()
        .forEach(defaultParameters::put);
    pgBouncer.set("defaultParameters", defaultParameters);
    status.set("pgBouncer", pgBouncer);
    resource.set("status", status);
    return resource;
  }

  ObjectNode getVersion1beta1Resource() {
    ObjectNode resource = JsonUtil.readFromJsonAsJson("pooling_config/version1beta1.json");
    ObjectNode parameters = (ObjectNode) resource.get("spec").get("pgBouncer").get("pgbouncer.ini");
    ObjectNode status = MAPPER.createObjectNode();
    ObjectNode pgBouncer = MAPPER.createObjectNode();
    ArrayNode defaultParameters = MAPPER.createArrayNode();
    PgBouncerDefaultValues.getDefaultValues()
        .entrySet()
        .stream()
        .filter(parameter -> !parameters.has(parameter.getKey())
            || parameters.get(parameter.getKey()).asText()
                .equals(parameter.getValue()))
        .forEach(parameter -> defaultParameters.add(parameter.getKey()));
    pgBouncer.set("defaultParameters", defaultParameters);
    status.set("pgBouncer", pgBouncer);
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