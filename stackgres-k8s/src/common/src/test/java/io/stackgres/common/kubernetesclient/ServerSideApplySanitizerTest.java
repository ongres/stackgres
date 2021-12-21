/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServerSideApplySanitizerTest {

  private final ObjectNode openApi = JsonUtil
      .readFromJsonXzAsJson("openapi.json.xz");

  @ParameterizedTest
  @ValueSource(strings = {
      "endpoints1.json",
      "endpoints2.json",
      "endpoints3.json",
      "role1.json",
      "role2.json",
      "role3.json",
      "rolebinding1.json",
      "rolebinding2.json",
      "rolebinding3.json",
      "secret1.json",
      "secret2.json",
      "secret3.json",
      "servicemonitor.json",
  })
  void testSanitizerReturnsSameIntent(String resourceFile) {
    var resource = JsonUtil.readFromJsonAsJson(
        "ssa-sanitization/" + resourceFile);
    var actualResult = new ServerSideApplySanitizer(openApi).sanitize(resource);
    JsonUtil.assertJsonEquals(
        Serialization.asJson(resource),
        Serialization.asJson(actualResult));
  }

  @ParameterizedTest
  @CsvSource({
    "sts1.json,sts1-sanitized.json",
    "sts2.json,sts2-sanitized.json",
    "sts3.json,sts3-sanitized.json",
    "sts4.json,sts4-sanitized.json",
    "svc1.json,svc1-sanitized.json",
    "svc2.json,svc2-sanitized.json",
    "svc3.json,svc3-sanitized.json",
    "serviceaccount.json,serviceaccount-sanitized.json"
  })
  void testSanitizerReturnsSanitizedIntent(String resourceFile, String sanitizedResourceFile) {
    var resource = JsonUtil.readFromJsonAsJson(
        "ssa-sanitization/" + resourceFile);
    var sanitizedResource = JsonUtil.readFromJsonAsJson(
        "ssa-sanitization/" + sanitizedResourceFile);
    var actualResult = new ServerSideApplySanitizer(openApi).sanitize(resource);
    JsonUtil.assertJsonEquals(
        Serialization.asJson(sanitizedResource),
        Serialization.asJson(actualResult));
  }

}
