/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.api.Test;

public class ExtensionsModelIgnoreUnknownPropertiesTest {

  @Test
  void extensionsModelShouldInoreUnknownProperties() {
    ModelTestUtil.assertJsonInoreUnknownProperties(StackGresExtensions.class);
  }

}
