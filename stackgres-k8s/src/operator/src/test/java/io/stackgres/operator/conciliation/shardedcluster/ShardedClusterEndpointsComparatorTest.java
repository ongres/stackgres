/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.Test;

class ShardedClusterEndpointsComparatorTest {

  private final ShardedClusterEndpointsComparator comparator =
      new ShardedClusterEndpointsComparator();

  private final Endpoints required = Fixtures.endpoints().loadRequired().get();

  private final Endpoints deployed = Fixtures.endpoints().loadDeployed().get();

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

  private Map<String, String> getAnnotations(String key, String value) {
    return ImmutableMap.<String, String>builder()
        .put(key, value)
        .build();
  }
}
