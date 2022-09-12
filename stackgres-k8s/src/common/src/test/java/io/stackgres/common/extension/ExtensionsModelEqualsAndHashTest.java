/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.api.Test;

public class ExtensionsModelEqualsAndHashTest {

  @Test
  void extensionModelShouldHaveEqualsAndHash() {
    var resource = ModelTestUtil.createWithRandomData(StackGresExtensions.class);
    ModelTestUtil.assertEqualsAndHashCode(resource);
    var anotherResource = ModelTestUtil.createWithRandomData(StackGresExtensions.class);
    assertNotEquals(anotherResource, resource);
  }

}
