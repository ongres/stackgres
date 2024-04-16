/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.annotation.Nonnull;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;

public interface PostgresConfigurationFactory
    extends DefaultCustomResourceFactory<StackGresPostgresConfig> {

  @Nonnull String getPostgresVersion();
}
