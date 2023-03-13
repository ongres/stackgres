/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StringUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Test;

class ShardedClusterSecretComparatorTest {

  private final ShardedClusterSecretComparator comparator = new ShardedClusterSecretComparator();

  @Test
  void exactlyTheSameContent_shouldReturnTrue() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withData(ImmutableMap.of("test", "test"))
            .build(),
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withData(ImmutableMap.of("test", "test"))
            .build());

    assertTrue(isTheSameContent);

  }

  @Test
  void givenStringDataAndTheSameData_shouldReturnTrue() {

    final String randomString = StringUtil.generateRandom();
    boolean isTheSameContent = comparator.isResourceContentEqual(
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withStringData(ImmutableMap.of("test", randomString))
            .build(),
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withData(ImmutableMap.of("test",
                ResourceUtil.encodeSecret(randomString)))
            .build());

    assertTrue(isTheSameContent);

  }

  @Test
  void givenDifferentStringDataAndData_shouldReturnFalse() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withStringData(ImmutableMap.of("test", StringUtil.generateRandom()))
            .build(),
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withData(ImmutableMap.of("test",
                ResourceUtil.encodeSecret(StringUtil.generateRandom())))
            .build());

    assertFalse(isTheSameContent);

  }

  @Test
  void givenEqualStringData_shouldReturnTrue() {
    final String randomString = StringUtil.generateRandom();
    boolean isTheSameContent = comparator.isResourceContentEqual(
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withStringData(ImmutableMap.of("test", randomString))
            .build(),
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withStringData(ImmutableMap.of("test", randomString))
            .build());

    assertTrue(isTheSameContent);
  }

  @Test
  void givenDifferentData_shouldReturnFalse() {

    boolean isTheSameContent = comparator.isResourceContentEqual(
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withData(ImmutableMap.of("test", StringUtil.generateRandom()))
            .build(),
        new SecretBuilder()
            .withNewMetadata().withNamespace("test")
            .withName("test")
            .endMetadata()
            .withData(ImmutableMap.of(
                "test", ResourceUtil.encodeSecret(StringUtil.generateRandom())))
            .build());

    assertFalse(isTheSameContent);
  }

}
