/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import static java.lang.String.format;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.testutil.JsonUtil;

public class SecretFixture {

  public Secret build(String jsonFilename) {
    return JsonUtil.readFromJson(format("secret/%s.json", jsonFilename), Secret.class);
  }

}
