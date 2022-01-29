/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SgPostgresConfigConversionPipelineTest {

  protected static final ObjectMapper MAPPER = JsonUtil.JSON_MAPPER;

  @Inject
  @Conversion(StackGresPostgresConfig.KIND)
  protected ConversionPipeline pipeline;

  ObjectNode getFromVersion1Resource() {
    return JsonUtil.readFromJsonAsJson("postgres_config/from_version1.json");
  }

  ObjectNode getToVersion1beta1Resource() {
    return JsonUtil.readFromJsonAsJson("postgres_config/to_version1beta1.json");
  }

  ObjectNode getFromVersion1beta1Resource() {
    return JsonUtil.readFromJsonAsJson("postgres_config/from_version1beta1.json");
  }

  ObjectNode getToVersion1Resource() {
    return JsonUtil.readFromJsonAsJson("postgres_config/to_version1.json");
  }

  @Test
  void resourceConversionWithBackupFromVersion1beta1ToVersion1_shouldNotFail() {
    ObjectNode fromVersion1beta1 = getFromVersion1beta1Resource();
    ObjectNode toVersion1 = getToVersion1Resource();
    JsonUtil.assertJsonEquals(toVersion1,
        pipeline.convert(
            ConversionUtil.API_VERSION_1,
            ImmutableList.of(fromVersion1beta1)).get(0));
  }

  @Test
  void resourceConversionWithBackupFromVersion1ToVersion1beta1_shouldNotFail() {
    ObjectNode fromVersion1 = getFromVersion1Resource();
    ObjectNode toVersion1beta1 = getToVersion1beta1Resource();
    JsonUtil.assertJsonEquals(toVersion1beta1,
        pipeline.convert(
            ConversionUtil.API_VERSION_1BETA1,
            ImmutableList.of(fromVersion1)).get(0));
  }

}
