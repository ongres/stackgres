/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.JsonUtil;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class SgPostgresConfigConversionPipelineTest {

  @Inject
  @Conversion(StackGresPostgresConfig.KIND)
  protected ConversionPipeline pipeline;

  ObjectNode getFromVersion1Resource() {
    return Fixtures.jsonPostgresConfig().loadFromVersion1().get();
  }

  ObjectNode getToVersion1beta1Resource() {
    return Fixtures.jsonPostgresConfig().loadToVersion1beta1().get();
  }

  ObjectNode getFromVersion1beta1Resource() {
    return Fixtures.jsonPostgresConfig().loadFromVersion1beta1().get();
  }

  ObjectNode getToVersion1Resource() {
    return Fixtures.jsonPostgresConfig().loadToVersion1().get();
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
    List<ObjectNode> converted = pipeline.convert(
        ConversionUtil.API_VERSION_1BETA1,
        ImmutableList.of(fromVersion1));
    JsonUtil.sortArray(toVersion1beta1.get("status").get("defaultParameters"));
    JsonUtil.sortArray(converted.getFirst().get("status").get("defaultParameters"));
    JsonUtil.assertJsonEquals(toVersion1beta1,
        converted.getFirst());
  }

}
