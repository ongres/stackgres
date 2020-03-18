/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.test.Mock;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.utils.JsonUtil;

public class MockPostgresConfigFinder implements CustomResourceFinder<StackGresPostgresConfig> {

  @Override
  public Optional<StackGresPostgresConfig> findByNameAndNamespace(String name, String namespace) {
    return Optional.of(JsonUtil.readFromJson("postgres_config/default_postgres.json", StackGresPostgresConfig.class));
  }
}
