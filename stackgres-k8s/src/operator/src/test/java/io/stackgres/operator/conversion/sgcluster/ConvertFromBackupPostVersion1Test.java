/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgcluster;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.operator.conversion.ConversionUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConvertFromBackupPostVersion1Test {

  protected static final JsonMapper MAPPER = new JsonMapper();

  protected ConvertFromBackupPostVersion1 converter;
  protected ObjectNode version1Cluster;
  protected ObjectNode version1beta1Cluster;

  ObjectNode getVersion1Resource() {
    return JsonUtil.readFromJsonAsJson("stackgres_cluster/version1.json");
  }

  ObjectNode getVersion1beta1Resource() {
    return JsonUtil.readFromJsonAsJson("stackgres_cluster/version1beta1.json");
  }

  @BeforeEach
  void setUp() {
    converter = new ConvertFromBackupPostVersion1();
    version1Cluster = getVersion1Resource();
    version1beta1Cluster = getVersion1beta1Resource();
  }

  @Test
  void resourceConversionWithBackupFromVersion1beta1ToVersion1_shouldNotFail() {
    version1Cluster.put("apiVersion", ConversionUtil.API_VERSION_1BETA1);
    JsonUtil.assertJsonEquals(version1Cluster,
        converter.convert(
            ConversionUtil.apiVersionAsNumberOf(version1beta1Cluster),
            ConversionUtil.VERSION_1,
            version1beta1Cluster));
  }

  @Test
  void resourceConversionWithBackupFromVersion1ToVersion1beta1_shouldNotFail() {
    version1beta1Cluster.put("apiVersion", ConversionUtil.API_VERSION_1);
    JsonUtil.assertJsonEquals(version1beta1Cluster,
        converter.convert(
            ConversionUtil.apiVersionAsNumberOf(version1Cluster),
            ConversionUtil.VERSION_1BETA1,
            version1Cluster));
  }

}
