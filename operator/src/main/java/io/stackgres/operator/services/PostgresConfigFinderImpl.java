/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.app.KubernetesClientFactory;

@ApplicationScoped
public class PostgresConfigFinderImpl implements PostgresConfigFinder {

  private KubernetesClientFactory kubClientFactory;

  @Inject
  public PostgresConfigFinderImpl(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<StackGresPostgresConfig> findPostgresConfig(String postgresProfile) {
    return Optional.empty();
  }

}
