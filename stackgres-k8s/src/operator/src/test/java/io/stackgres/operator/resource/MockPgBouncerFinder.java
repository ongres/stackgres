/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.testutil.JsonUtil;

//@Mock
public class MockPgBouncerFinder implements CustomResourceFinder<StackGresPoolingConfig> {
  @Override
  public Optional<StackGresPoolingConfig> findByNameAndNamespace(String name, String namespace) {
    return Optional.of(JsonUtil
        .readFromJson("pooling_config/default.json", StackGresPoolingConfig.class));
  }
}
