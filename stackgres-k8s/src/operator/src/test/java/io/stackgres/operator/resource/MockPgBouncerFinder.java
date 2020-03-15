/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.test.Mock;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.utils.JsonUtil;

public class MockPgBouncerFinder implements CustomResourceFinder<StackGresPgbouncerConfig> {
  @Override
  public Optional<StackGresPgbouncerConfig> findByNameAndNamespace(String name, String namespace) {
    return Optional.of(JsonUtil
        .readFromJson("pgbouncer_config/default.json", StackGresPgbouncerConfig.class));
  }
}
