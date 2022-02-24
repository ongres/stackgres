/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SgPoolingConfigConversionPipelineTest {

  @Inject
  @Conversion(StackGresPoolingConfig.KIND)
  protected ConversionPipeline pipeline;

  ObjectNode getFromVersion1Resource() {
    return JsonUtil.readFromJsonAsJson("pooling_config/from_version1.json");
  }

  ObjectNode getToVersion1beta1Resource() {
    return JsonUtil.readFromJsonAsJson("pooling_config/to_version1beta1.json");
  }

  ObjectNode getFromVersion1beta1Resource() {
    return JsonUtil.readFromJsonAsJson("pooling_config/from_version1beta1.json");
  }

  ObjectNode getToVersion1Resource() {
    return JsonUtil.readFromJsonAsJson("pooling_config/to_version1.json");
  }

  @Test
  void resourceConversionWithBackupFromVersion1beta1ToVersion1_shouldNotFail() {
    ObjectNode fromVersion1beta1 = getFromVersion1beta1Resource();
    ObjectNode toVersion1 = getToVersion1Resource();

    var converted = pipeline.convert(ConversionUtil.API_VERSION_1, List.of(fromVersion1beta1));
    JsonUtil.assertJsonEquals(toVersion1, converted.get(0));
  }

  @Test
  void resourceConversionWithBackupFromVersion1ToVersion1beta1_shouldNotFail() {
    ObjectNode fromVersion1 = getFromVersion1Resource();
    ObjectNode toVersion1beta1 = getToVersion1beta1Resource();

    var converted = pipeline.convert(ConversionUtil.API_VERSION_1BETA1, List.of(fromVersion1));
    JsonUtil.assertJsonEquals(toVersion1beta1, converted.get(0));
  }

}
