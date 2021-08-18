/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SgClusterConversionPipelineTest {

  protected static final JsonMapper MAPPER = new JsonMapper();

  @Inject
  @Conversion(StackGresCluster.KIND)
  protected ConversionPipeline pipeline;

  ObjectNode getFromVersion1Resource() {
    return JsonUtil.readFromJsonAsJson("stackgres_cluster/from_version1.json");
  }

  ObjectNode getToVersion1beta1Resource() {
    return JsonUtil.readFromJsonAsJson("stackgres_cluster/to_version1beta1.json");
  }

  ObjectNode getFromVersion1beta1Resource() {
    return JsonUtil.readFromJsonAsJson("stackgres_cluster/from_version1beta1.json");
  }

  ObjectNode getToVersion1Resource() {
    return JsonUtil.readFromJsonAsJson("stackgres_cluster/to_version1.json");
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

}
