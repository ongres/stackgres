/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResourceUtilTest {

  @Test
  void sanitizeResourceName() {
    Assertions.assertEquals(
        "a-v3ry-str-nge-resource-name-th4t-hav-to-be-s-n-tized",
        ResourceUtil.sanitizedResourceName(
            "#A-v3ry-str@nge_Resource, name th4t hav€ to be sªn¡tized!"));
  }

}
