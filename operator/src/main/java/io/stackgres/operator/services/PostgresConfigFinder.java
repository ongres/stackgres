/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import java.util.Optional;

import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;

public interface PostgresConfigFinder {

  Optional<StackGresPostgresConfig> findPostgresConfig(String postgresProfile);
}
