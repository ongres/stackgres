/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

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
    ModelTestUtil.assertEqualsAndHashCode(resourceClazz);
  }
}
