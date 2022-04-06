/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

class ClusterEndpointComparatorTest {

  private static final ObjectMapper JSON_MAPPER = JsonUtil.JSON_MAPPER;

  private final ClusterEndpointComparator comparator = new ClusterEndpointComparator();

  private final Map<String, String> defaultConfig = ImmutableMap
      .of("ttl", "30", "loop_wait", "10");

  private final Map<String, String> defaultAnnotations = ImmutableMap
      .of("config", JSON_MAPPER.valueToTree(defaultConfig).toString());

  private final Endpoints required = JsonUtil
      .readFromJson("endpoints/required.json", Endpoints.class);

  private final Endpoints deployed = JsonUtil
      .readFromJson("endpoints/deployed.json", Endpoints.class);

  @Test
  void generatedResourceAndRequiredResource_shouldHaveNoDifference() {
    var isContentEqual = comparator.isResourceContentEqual(required, deployed);

    assertTrue(isContentEqual);
  }

  @Test
  void isEndpointIsExactlyTheSame_itShouldReturnTrue() {
    var isSameContent = comparator.isResourceContentEqual(
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .withAnnotations(getAnnotations("initialize", "6889156288560377979"))
            .endMetadata()
            .build(),
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .withAnnotations(getAnnotations("initialize", "6889156288560377979"))
            .endMetadata()
            .build());

    assertTrue(isSameContent);
  }

  @Test
  void isInitializeNotSet_itShouldIgnoreTheDifference() {
    var isSameContent = comparator.isResourceContentEqual(
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .withAnnotations(defaultAnnotations)
            .endMetadata()
            .build(),
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .withAnnotations(getAnnotations("initialize", "6889156288560377979"))
            .endMetadata()
            .build());

    assertTrue(isSameContent);
  }

  @Test
  void isInitializeIsDifferent_itShouldIgnoreTheDifference() {
    var isSameContent = comparator.isResourceContentEqual(
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .withAnnotations(getAnnotations("initialize", "6889156288560377970"))
            .endMetadata()
            .build(),
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .withAnnotations(getAnnotations("initialize", "6889156288560377979"))
            .endMetadata()
            .build());

    assertFalse(isSameContent);
  }

  @Test
  void lockingAnnotaionsShouldBeIgnored() {
    var isSameContent = comparator.isResourceContentEqual(
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .endMetadata()
            .build(),
        new EndpointsBuilder()
            .withNewMetadata()
            .withName("test")
            .withNamespace("test")
            .withAnnotations(Map.of(
                "acquireTime", "2021-01-15T23,54,37.338546+00,00",
                "leader", "dbops-restart-0",
                "optime", "50372080",
                "renewTime", "2021-01-15T23,54,37.395964+00,00",
                "transitions", "0",
                "ttl", "30"))
            .endMetadata()
            .build());

    assertTrue(isSameContent);
  }

  private Map<String, String> getAnnotations(String key, String value) {
    return ImmutableMap.<String, String>builder()
        .putAll(defaultAnnotations)
        .put(key, value)
        .build();
  }
}
