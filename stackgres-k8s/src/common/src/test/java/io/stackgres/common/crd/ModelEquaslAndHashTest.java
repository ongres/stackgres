/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ModelEquaslAndHashTest {

  @ParameterizedTest
  @ValueSource(classes = {
      Volume.class,
      VolumeMount.class
  })
  void shouldHaveEqualsAndHash(Class<?> resourceClazz) {
    var resource = ModelTestUtil.createWithRandomData(resourceClazz);
    ModelTestUtil.assertEqualsAndHashCode(resource);
    var anotherResource = ModelTestUtil.createWithRandomData(resourceClazz);
    assertNotEquals(anotherResource, resource);
  }
}
